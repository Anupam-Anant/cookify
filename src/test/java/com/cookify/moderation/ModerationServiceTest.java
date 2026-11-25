package com.cookify.moderation;

import com.cookify.category.RecipeCategoryRepository;
import com.cookify.chat.ChatMessageRepository;
import com.cookify.rating.RecipeRating;
import com.cookify.rating.RecipeRatingRepository;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.saved.SavedRecipeRepository;
import com.cookify.subscription.Subscription;
import com.cookify.subscription.SubscriptionRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest: all collaborators are plain JPA repositories, so ModerationService is
// constructed directly against the real (H2) repos — same pattern as OtpServiceTest /
// RatingServiceTest. No EmailService/ContentFilter needed here.
@DataJpaTest
class ModerationServiceTest {

    @Autowired WarningRepository warnings;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired RecipeCategoryRepository categories;
    @Autowired RecipeRatingRepository ratings;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired SavedRecipeRepository savedRecipes;
    @Autowired ChatMessageRepository chatMessages;

    ModerationService service;

    @BeforeEach
    void setUp() {
        service = new ModerationService(warnings, users, recipes, categories, ratings, subscriptions,
            savedRecipes, chatMessages);
    }

    private Long seedUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPhone("+91" + Math.abs(username.hashCode() % 1000000000));
        u.setPasswordHash("$2a$dummy");
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(u).getId();
    }

    @Test
    void twoWarningsDoNotBan() {
        Long userId = seedUser("offender1");

        service.warn(userId, "first strike");
        service.warn(userId, "second strike");

        User reloaded = users.findById(userId).orElseThrow();
        assertThat(reloaded.getWarningCount()).isEqualTo(2);
        assertThat(reloaded.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(service.isBanned(userId)).isFalse();
    }

    @Test
    void thirdWarningBansAndPurgesContent() {
        Long offenderId = seedUser("offender2");
        Long otherUserId = seedUser("bystander2");

        // offender's own recipe, with a rating from the bystander, plus a category row.
        Recipe recipe = RecipeFactory.create(true);
        recipe.setRecipeName("Offender's Recipe");
        recipe.setRecipeCreatorId(offenderId);
        Long recipeId = recipes.save(recipe).getId();

        RecipeRating ratingOnOffendersRecipe = new RecipeRating();
        ratingOnOffendersRecipe.setRecipeId(recipeId);
        ratingOnOffendersRecipe.setUserId(otherUserId);
        ratingOnOffendersRecipe.setRate5(1);
        ratings.save(ratingOnOffendersRecipe);

        // offender also rated/commented on someone else's recipe.
        Recipe othersRecipe = RecipeFactory.create(false);
        othersRecipe.setRecipeName("Bystander's Recipe");
        othersRecipe.setRecipeCreatorId(otherUserId);
        Long othersRecipeId = recipes.save(othersRecipe).getId();

        RecipeRating offenderRatingElsewhere = new RecipeRating();
        offenderRatingElsewhere.setRecipeId(othersRecipeId);
        offenderRatingElsewhere.setUserId(offenderId);
        offenderRatingElsewhere.setRate3(1);
        offenderRatingElsewhere.setComments("nice");
        ratings.save(offenderRatingElsewhere);

        // offender subscribes to bystander, and bystander subscribes to offender.
        Subscription asSubscriber = new Subscription();
        asSubscriber.setSubscriberId(offenderId);
        asSubscriber.setCreatorId(otherUserId);
        subscriptions.save(asSubscriber);

        Subscription asCreator = new Subscription();
        asCreator.setSubscriberId(otherUserId);
        asCreator.setCreatorId(offenderId);
        subscriptions.save(asCreator);

        service.warn(offenderId, "strike one");
        service.warn(offenderId, "strike two");
        service.warn(offenderId, "strike three");

        User reloaded = users.findById(offenderId).orElseThrow();
        assertThat(reloaded.getWarningCount()).isEqualTo(3);
        assertThat(reloaded.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(service.isBanned(offenderId)).isTrue();

        assertThat(warnings.countByUserId(offenderId)).isEqualTo(3);

        // offender's recipe + its category + its ratings are purged.
        assertThat(recipes.findById(recipeId)).isEmpty();
        assertThat(categories.findById(recipeId)).isEmpty();
        assertThat(ratings.findByRecipeId(recipeId)).isEmpty();

        // offender's own rating row on someone else's (still-existing) recipe is purged.
        assertThat(ratings.findByRecipeIdAndUserId(othersRecipeId, offenderId)).isEmpty();
        assertThat(recipes.findById(othersRecipeId)).isPresent();

        // subscriptions on both sides are purged.
        assertThat(subscriptions.existsBySubscriberIdAndCreatorId(offenderId, otherUserId)).isFalse();
        assertThat(subscriptions.existsBySubscriberIdAndCreatorId(otherUserId, offenderId)).isFalse();
    }
}

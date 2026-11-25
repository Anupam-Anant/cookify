package com.cookify.comment;

import com.cookify.moderation.WarningRepository;
import com.cookify.rating.RecipeRating;
import com.cookify.rating.RecipeRatingRepository;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.subscription.Subscription;
import com.cookify.subscription.SubscriptionRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fix-round-1 regression coverage: proves (against the REAL Spring-wired
 * CommentService/ModerationService, not mocks) that an NSFW comment's moderation
 * side-effects (warning row, warningCount++, and the 3rd-strike ban+purge) commit
 * even though CommentService.addComment rolls back its own @Transactional method by
 * throwing CommentRejectedException afterwards.
 *
 * Before the fix, ModerationService.warn ran with default REQUIRED propagation and
 * joined addComment's transaction, so the subsequent throw silently rolled back the
 * warning/ban too — NSFW strikes never actually accumulated. ModerationService.warn is
 * now @Transactional(propagation = REQUIRES_NEW), so it commits independently.
 *
 * @SpringBootTest against the real, shared (non-rolled-back) H2 "cookify" instance —
 * same pattern as RecipeSocialControllerTest. Usernames/phones below are chosen to be
 * globally unique against every other test class's seed data in that shared instance.
 */
@SpringBootTest
class CommentServiceModerationPersistenceTest {

    @Autowired CommentService commentService;
    @Autowired WarningRepository warnings;
    @Autowired UserRepository users;
    @Autowired RecipeRatingRepository ratings;
    @Autowired RecipeRepository recipes;
    @Autowired SubscriptionRepository subscriptions;

    private User seedUser(String username, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPhone(phone);
        u.setPasswordHash("$2a$dummy");
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(u);
    }

    private Recipe seedRecipe(String name, Long creatorId) {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName(name);
        r.setRecipeCreatorId(creatorId);
        return recipes.save(r);
    }

    @Test
    void nsfwCommentPersistsWarningDespiteRejectionRollback() {
        User creator = seedUser("modfixcreatorA", "+917777777791");
        User offender = seedUser("modfixoffenderA", "+917777777792");
        Recipe recipe = seedRecipe("Fix Round 1 Recipe A", creator.getId());

        assertThatThrownBy(() ->
            commentService.addComment(recipe.getId(), offender.getId(), "this is badword1 content"))
            .isInstanceOf(CommentRejectedException.class);

        // The warning + warningCount++ must have committed, NOT been rolled back by the
        // exception addComment threw afterwards in its own (now-abandoned) transaction.
        assertThat(warnings.countByUserId(offender.getId())).isEqualTo(1);
        User reloaded = users.findById(offender.getId()).orElseThrow();
        assertThat(reloaded.getWarningCount()).isEqualTo(1);
        assertThat(reloaded.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);

        // No comment/rating row was stored for the rejected text.
        assertThat(ratings.findByRecipeIdAndUserId(recipe.getId(), offender.getId())).isEmpty();
    }

    @Test
    void thirdNsfwCommentPersistsBanAndPurgeDespiteRejectionRollback() {
        User creator = seedUser("modfixcreatorB", "+917777777793");
        User offender = seedUser("modfixoffenderB", "+917777777794");

        Recipe creatorRecipe = seedRecipe("Fix Round 1 Recipe B", creator.getId());
        Recipe offenderRecipe = seedRecipe("Offender's Own Recipe B", offender.getId());

        RecipeRating offenderRatingOnOthersRecipe = new RecipeRating();
        offenderRatingOnOthersRecipe.setRecipeId(creatorRecipe.getId());
        offenderRatingOnOthersRecipe.setUserId(offender.getId());
        offenderRatingOnOthersRecipe.setRate4(1);
        ratings.save(offenderRatingOnOthersRecipe);

        Subscription offenderSubscribesToCreator = new Subscription();
        offenderSubscribesToCreator.setSubscriberId(offender.getId());
        offenderSubscribesToCreator.setCreatorId(creator.getId());
        subscriptions.save(offenderSubscribesToCreator);

        Subscription creatorSubscribesToOffender = new Subscription();
        creatorSubscribesToOffender.setSubscriberId(creator.getId());
        creatorSubscribesToOffender.setCreatorId(offender.getId());
        subscriptions.save(creatorSubscribesToOffender);

        for (int i = 0; i < 3; i++) {
            String text = "badword1 strike " + i;
            assertThatThrownBy(() ->
                commentService.addComment(creatorRecipe.getId(), offender.getId(), text))
                .isInstanceOf(CommentRejectedException.class);
        }

        assertThat(warnings.countByUserId(offender.getId())).isEqualTo(3);
        User reloaded = users.findById(offender.getId()).orElseThrow();
        assertThat(reloaded.getWarningCount()).isEqualTo(3);
        assertThat(reloaded.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);

        // banAndPurge committed: offender's own recipe is gone, their rating on the
        // still-existing creator recipe is gone, and both-direction subscriptions are gone.
        assertThat(recipes.findById(offenderRecipe.getId())).isEmpty();
        assertThat(recipes.findById(creatorRecipe.getId())).isPresent();
        assertThat(ratings.findByRecipeIdAndUserId(creatorRecipe.getId(), offender.getId())).isEmpty();
        assertThat(subscriptions.existsBySubscriberIdAndCreatorId(offender.getId(), creator.getId())).isFalse();
        assertThat(subscriptions.existsBySubscriberIdAndCreatorId(creator.getId(), offender.getId())).isFalse();
    }
}

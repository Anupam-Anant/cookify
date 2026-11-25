package com.cookify.rating;

import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @DataJpaTest: seeds real Recipe/User rows and uses their generated ids, because
// recipe_rating.recipe_id/user_id are FK-constrained (same pattern as OtpServiceTest /
// RecipeRepositoryTest). RatingService is constructed directly (no EmailService/ContentFilter
// beans are loaded by @DataJpaTest, and none are needed here anyway).
@DataJpaTest
class RatingServiceTest {

    @Autowired RecipeRatingRepository ratingRepo;
    @Autowired RecipeRepository recipes;
    @Autowired UserRepository users;

    RatingService service;
    Long recipeId;

    @BeforeEach
    void setUp() {
        service = new RatingService(ratingRepo);

        Long creatorId = seedUser("chef1");
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Paneer Tikka");
        r.setRecipeCreatorId(creatorId);
        recipeId = recipes.save(r).getId();
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
    void singleRatingProducesItsOwnAverage() {
        Long userId = seedUser("rater1");
        service.rate(recipeId, userId, 4);

        RatingSummary summary = service.summary(recipeId);

        assertThat(summary.average()).isEqualTo(4.0);
        assertThat(summary.count()).isEqualTo(1);
    }

    @Test
    void secondUserRatingAveragesAcrossBoth() {
        Long user1 = seedUser("rater2a");
        Long user2 = seedUser("rater2b");
        service.rate(recipeId, user1, 4);
        service.rate(recipeId, user2, 2);

        RatingSummary summary = service.summary(recipeId);

        assertThat(summary.average()).isEqualTo(3.0);
        assertThat(summary.count()).isEqualTo(2);
    }

    @Test
    void reRatingBySameUserReplacesRatherThanAdds() {
        Long userId = seedUser("rater3");
        service.rate(recipeId, userId, 1);
        service.rate(recipeId, userId, 5);

        RatingSummary summary = service.summary(recipeId);

        assertThat(summary.average()).isEqualTo(5.0);
        assertThat(summary.count()).isEqualTo(1);
    }

    @Test
    void starOutOfRangeThrows() {
        Long userId = seedUser("rater4");
        assertThatThrownBy(() -> service.rate(recipeId, userId, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.rate(recipeId, userId, 6))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void summariesBatchMatchesIndividualSummariesIncludingAZeroRatingRecipe() {
        Long creatorId = seedUser("chef5");
        Recipe second = RecipeFactory.create(true);
        second.setRecipeName("Second Recipe");
        second.setRecipeCreatorId(creatorId);
        Long secondRecipeId = recipes.save(second).getId();

        Recipe third = RecipeFactory.create(true);
        third.setRecipeName("Unrated Recipe");
        third.setRecipeCreatorId(creatorId);
        Long unratedRecipeId = recipes.save(third).getId();

        Long user1 = seedUser("batchrater1");
        Long user2 = seedUser("batchrater2");
        service.rate(recipeId, user1, 4);
        service.rate(recipeId, user2, 2);
        service.rate(secondRecipeId, user1, 5);

        Map<Long, RatingSummary> batch = service.summaries(List.of(recipeId, secondRecipeId, unratedRecipeId));

        assertThat(batch.get(recipeId)).isEqualTo(service.summary(recipeId));
        assertThat(batch.get(secondRecipeId)).isEqualTo(service.summary(secondRecipeId));
        assertThat(batch.get(unratedRecipeId)).isEqualTo(new RatingSummary(0.0, 0));
    }
}

package com.cookify.search;

import com.cookify.rating.RatingService;
import com.cookify.rating.RecipeRatingRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest: seeds real Recipe/User rows (same pattern as RatingServiceTest) and exercises
// RecipeRepository's JpaSpecificationExecutor support directly against H2.
@DataJpaTest
class RecipeSearchServiceTest {

    @Autowired RecipeRepository recipes;
    @Autowired RecipeRatingRepository ratingRepo;
    @Autowired UserRepository users;

    RecipeSearchService service;
    Long chefId;

    @BeforeEach
    void setUp() {
        service = new RecipeSearchService(recipes, new RatingService(ratingRepo));
        chefId = seedUser("search6chef");
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

    private Recipe seedRecipe(String name, boolean veg, Integer cost, long views) {
        Recipe r = RecipeFactory.create(veg);
        r.setRecipeName(name);
        r.setRecipeCreatorId(chefId);
        r.setCost(cost);
        r.setViews(views);
        return recipes.save(r);
    }

    @Test
    void vegFilterReturnsOnlyVegRecipes() {
        seedRecipe("Veg Curry", true, 100, 10);
        seedRecipe("Chicken Curry", false, 100, 10);

        RecipeFilter filter = new RecipeFilter();
        filter.setVeg(true);
        List<Recipe> result = service.search(filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).dietaryLabel()).isEqualTo("Veg");
    }

    @Test
    void nonVegFilterReturnsOnlyNonVegRecipes() {
        seedRecipe("Veg Curry 2", true, 100, 10);
        seedRecipe("Chicken Curry 2", false, 100, 10);

        RecipeFilter filter = new RecipeFilter();
        filter.setVeg(false);
        List<Recipe> result = service.search(filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).dietaryLabel()).isEqualTo("Non-Veg");
    }

    @Test
    void maxCostExcludesPricierRecipes() {
        seedRecipe("Cheap Snack", true, 50, 5);
        seedRecipe("Pricey Feast", true, 500, 5);

        RecipeFilter filter = new RecipeFilter();
        filter.setMaxCost(100);
        List<Recipe> result = service.search(filter);

        assertThat(result).extracting(Recipe::getRecipeName).containsExactly("Cheap Snack");
    }

    @Test
    void popularSortOrdersHigherViewsAndRatingFirst() {
        Recipe popular = seedRecipe("Crowd Favorite", true, 100, 500);
        seedRecipe("Rarely Seen", true, 100, 1);
        Long raterId = seedUser("search6rater");
        new RatingService(ratingRepo).rate(popular.getId(), raterId, 5);

        RecipeFilter filter = new RecipeFilter();
        filter.setSort("popular");
        List<Recipe> result = service.search(filter);

        assertThat(result).extracting(Recipe::getRecipeName)
            .containsExactly("Crowd Favorite", "Rarely Seen");
    }
}

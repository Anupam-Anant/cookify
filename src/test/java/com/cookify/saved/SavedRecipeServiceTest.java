package com.cookify.saved;

import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest against the real (shared, non-rolled-back) H2 "cookify" instance —
// same pattern as ChatServiceTest/SubscriptionServiceTest. Usernames are unique per test
// method so re-running this class is idempotent.
@SpringBootTest
class SavedRecipeServiceTest {

    @Autowired SavedRecipeService savedRecipeService;
    @Autowired SavedRecipeRepository savedRecipeRepository;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;

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

    private Long seedRecipe(String name, Long creatorId) {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName(name);
        r.setRecipeCreatorId(creatorId);
        return recipes.save(r).getId();
    }

    @Test
    void saveIsIdempotentAndIsSavedReflectsState() {
        Long userId = seedUser("saver1");
        Long creatorId = seedUser("savecreator1");
        Long recipeId = seedRecipe("Saveable Curry", creatorId);

        assertThat(savedRecipeService.isSaved(userId, recipeId)).isFalse();

        savedRecipeService.save(userId, recipeId);
        savedRecipeService.save(userId, recipeId); // idempotent: must not throw / duplicate

        assertThat(savedRecipeService.isSaved(userId, recipeId)).isTrue();
        assertThat(savedRecipeRepository.findByUserIdOrderByCreatedAtDesc(userId)).hasSize(1);
    }

    @Test
    void unsaveRemovesItAndIsIdempotent() {
        Long userId = seedUser("saver2");
        Long creatorId = seedUser("savecreator2");
        Long recipeId = seedRecipe("Unsaveable Curry", creatorId);

        savedRecipeService.save(userId, recipeId);
        assertThat(savedRecipeService.isSaved(userId, recipeId)).isTrue();

        savedRecipeService.unsave(userId, recipeId);
        assertThat(savedRecipeService.isSaved(userId, recipeId)).isFalse();

        // Unsaving something never saved (or already unsaved) is a no-op, not an error.
        savedRecipeService.unsave(userId, recipeId);
    }

    @Test
    void savedRecipesListsOnlyWhatWasSaved() {
        Long userId = seedUser("saver3");
        Long creatorId = seedUser("savecreator3");
        Long saved = seedRecipe("Saved One", creatorId);
        Long notSaved = seedRecipe("Not Saved One", creatorId);

        savedRecipeService.save(userId, saved);

        List<Recipe> list = savedRecipeService.savedRecipes(userId);

        assertThat(list).extracting(Recipe::getId).containsExactly(saved);
    }
}

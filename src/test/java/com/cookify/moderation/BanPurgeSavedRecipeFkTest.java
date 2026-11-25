package com.cookify.moderation;

import com.cookify.chat.ChatMessage;
import com.cookify.chat.ChatMessageRepository;
import com.cookify.chat.ChatService;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.saved.SavedRecipeRepository;
import com.cookify.saved.SavedRecipeService;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Guards against a FK-constraint regression in {@code ModerationService.banAndPurge}:
 * {@code saved_recipe} has a FK {@code recipe_id} to {@code recipe} with no cascade (V4
 * migration), so deleting a banned user's recipes without first clearing anyone's bookmarks
 * of them throws a FK-constraint violation on {@code recipeRepository.deleteAll(ownRecipes)}.
 * Because {@code warn()} is {@code @Transactional(REQUIRES_NEW)} and calls {@code banAndPurge}
 * inline, that exception would silently roll back the whole transaction: the warning, the
 * warningCount increment, AND the ban all lost together.
 *
 * If this regresses: the 3rd {@code warn()} call below throws (FK violation), and the
 * offender is never actually suspended. With the bug fixed: the ban commits cleanly, the
 * offender's recipe and everyone's bookmarks of it are gone, the offender's own data is
 * purged, and the bystander's account and unrelated data are untouched.
 *
 * {@code @SpringBootTest} against the real, shared (non-rolled-back) H2 "cookify" instance —
 * same pattern as {@code CommentServiceModerationPersistenceTest}. Usernames/phones below are
 * chosen to be globally unique against every other test class's seed data in that shared
 * instance.
 */
@SpringBootTest
class BanPurgeSavedRecipeFkTest {

    @Autowired ModerationService moderationService;
    @Autowired SavedRecipeService savedRecipeService;
    @Autowired ChatService chatService;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired SavedRecipeRepository savedRecipes;
    @Autowired ChatMessageRepository chatMessages;

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
    void banCommitsAndPurgesBookmarksAndChatDespiteForeignFkOnSavedRecipe() {
        User offender = seedUser("fkfixoffender", "+917777778801");
        User bystander = seedUser("fkfixbystander", "+917777778802");

        // Offender's own recipe, bookmarked by the bystander.
        Recipe offenderRecipe = seedRecipe("FK Fix Offender Recipe", offender.getId());
        savedRecipeService.save(bystander.getId(), offenderRecipe.getId());

        // Offender also bookmarks someone else's recipe (the bystander's).
        Recipe bystanderRecipe = seedRecipe("FK Fix Bystander Recipe", bystander.getId());
        savedRecipeService.save(offender.getId(), bystanderRecipe.getId());

        // A DM between the two.
        ChatMessage dm = chatService.send(bystander.getId(), offender.getId(), "hello offender");

        // Drive the offender to the 3rd strike. Before the fix, this throws (FK violation on
        // saved_recipe) instead of completing, and the ban is rolled back along with it.
        assertThatCode(() -> {
            moderationService.warn(offender.getId(), "strike one");
            moderationService.warn(offender.getId(), "strike two");
            moderationService.warn(offender.getId(), "strike three");
        }).doesNotThrowAnyException();

        // The ban actually committed.
        User reloadedOffender = users.findById(offender.getId()).orElseThrow();
        assertThat(reloadedOffender.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(moderationService.isBanned(offender.getId())).isTrue();

        // Offender's recipe is gone, and so is the bystander's bookmark of it.
        assertThat(recipes.findById(offenderRecipe.getId())).isEmpty();
        assertThat(savedRecipes.existsByUserIdAndRecipeId(bystander.getId(), offenderRecipe.getId())).isFalse();

        // Offender's own bookmark (of the bystander's still-existing recipe) is gone too.
        assertThat(savedRecipes.existsByUserIdAndRecipeId(offender.getId(), bystanderRecipe.getId())).isFalse();

        // The DM between them is gone.
        assertThat(chatMessages.findById(dm.getId())).isEmpty();

        // Sanity: the bystander's account and their own recipe are untouched.
        User reloadedBystander = users.findById(bystander.getId()).orElseThrow();
        assertThat(reloadedBystander.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(recipes.findById(bystanderRecipe.getId())).isPresent();
    }
}

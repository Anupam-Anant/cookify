package com.cookify.user;

import com.cookify.auth.CookifyUserDetails;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.saved.SavedRecipeService;
import com.cookify.subscription.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired PasswordEncoder encoder;
    @Autowired SavedRecipeService savedRecipeService;
    @Autowired SubscriptionService subscriptionService;

    private User user;

    @BeforeEach
    void seed() {
        user = users.findByUsername("profileuser").orElseGet(() -> {
            User u = new User();
            u.setUsername("profileuser");
            u.setEmail("profile@example.com");
            u.setPhone("+912222222222");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });
    }

    @Test
    void updatesBio() throws Exception {
        var principal = new CookifyUserDetails(user);
        mvc.perform(post("/profile/edit").with(csrf()).with(user(principal))
                .param("bio", "I love cooking biryani"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/profile"));
        assertThat(users.findById(user.getId()).orElseThrow().getBio())
            .isEqualTo("I love cooking biryani");
    }

    @Test
    void enablingTwoFactorCheckboxPersistsTrue() throws Exception {
        user.setTwoFactorEnabled(false);
        users.save(user);
        var principal = new CookifyUserDetails(user);

        mvc.perform(post("/profile/edit").with(csrf()).with(user(principal))
                .param("twoFactorEnabled", "true"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/profile"));

        assertThat(users.findById(user.getId()).orElseThrow().isTwoFactorEnabled()).isTrue();
    }

    @Test
    void omittingTwoFactorCheckboxPersistsFalse() throws Exception {
        user.setTwoFactorEnabled(true);
        users.save(user);
        var principal = new CookifyUserDetails(user);

        mvc.perform(post("/profile/edit").with(csrf()).with(user(principal))
                .param("bio", "no 2fa param submitted"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/profile"));

        assertThat(users.findById(user.getId()).orElseThrow().isTwoFactorEnabled()).isFalse();
    }

    @Test
    void profilePageShowsUploadedAndSavedRecipesAndCounts() throws Exception {
        User otherChef = users.findByUsername("profileotherchef").orElseGet(() -> {
            User u = new User();
            u.setUsername("profileotherchef");
            u.setEmail("profileotherchef@example.com");
            u.setPhone("+912222222233");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });

        Recipe uploaded = RecipeFactory.create(true);
        uploaded.setRecipeName("My Own Uploaded Curry");
        uploaded.setRecipeCreatorId(user.getId());
        recipes.save(uploaded);

        Recipe toSave = RecipeFactory.create(true);
        toSave.setRecipeName("A Curry I Saved");
        toSave.setRecipeCreatorId(otherChef.getId());
        toSave = recipes.save(toSave);
        savedRecipeService.save(user.getId(), toSave.getId());

        subscriptionService.subscribe(user.getId(), otherChef.getId());

        var principal = new CookifyUserDetails(user);
        mvc.perform(get("/profile").with(user(principal)))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("My Own Uploaded Curry")))
           .andExpect(content().string(containsString("A Curry I Saved")))
           .andExpect(content().string(containsString("Following: 1")));
    }

    @Test
    void updateWithTooLongBioRerendersFormWithFieldErrorAndDoesNotPersist() throws Exception {
        var principal = new CookifyUserDetails(user);
        String longBio = "a".repeat(600);

        mvc.perform(post("/profile/edit").with(csrf()).with(user(principal))
                .param("bio", longBio))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("ck-error")));

        assertThat(users.findById(user.getId()).orElseThrow().getBio()).isNotEqualTo(longBio);
    }

    @Test
    void profilePageShowsAvatarImageWhenProfileImagePathSet() throws Exception {
        user.setProfileImagePath("avatars/profileuser.png");
        users.save(user);
        var principal = new CookifyUserDetails(user);

        mvc.perform(get("/profile").with(user(principal)))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("/uploads/")));
    }

    @Test
    void profilePageShowsEditAndDeleteControlsForOwnUploadedRecipes() throws Exception {
        Recipe uploaded = RecipeFactory.create(true);
        uploaded.setRecipeName("Editable Own Curry");
        uploaded.setRecipeCreatorId(user.getId());
        uploaded = recipes.save(uploaded);

        var principal = new CookifyUserDetails(user);
        mvc.perform(get("/profile").with(user(principal)))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("/recipes/" + uploaded.getId() + "/edit")))
           .andExpect(content().string(containsString("/recipes/" + uploaded.getId() + "/delete")));
    }
}

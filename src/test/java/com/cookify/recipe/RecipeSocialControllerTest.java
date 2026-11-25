package com.cookify.recipe;

import com.cookify.auth.CookifyUserDetails;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest + MockMvc against the real (shared, non-rolled-back) H2 "cookify" instance —
// same pattern as RecipeControllerTest/ExploreControllerTest. Usernames are looked up with
// orElseGet so re-running @BeforeEach across this class's several @Test methods is idempotent.
@SpringBootTest
@AutoConfigureMockMvc
class RecipeSocialControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired PasswordEncoder encoder;

    private User owner;
    private User visitor;

    @BeforeEach
    void seed() {
        owner = seedUser("social7owner", "social7owner@example.com", "+917777777781");
        visitor = seedUser("social7visitor", "social7visitor@example.com", "+917777777782");
    }

    private User seedUser(String username, String email, String phone) {
        return users.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPhone(phone);
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });
    }

    private Recipe seedRecipe(String name, User creator) {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName(name);
        r.setRecipeCreatorId(creator.getId());
        return recipes.save(r);
    }

    @Test
    void postRateUpdatesSummaryShownOnDetailPage() throws Exception {
        Recipe r = seedRecipe("Rate Me Curry", owner);

        mvc.perform(post("/recipes/" + r.getId() + "/rate")
                .param("stars", "5")
                .with(user(new CookifyUserDetails(visitor)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/recipes/" + r.getId()));

        mvc.perform(get("/recipes/" + r.getId()).with(user(new CookifyUserDetails(visitor))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("5.0")))
           .andExpect(content().string(containsString("1 rating")));
    }

    @Test
    void postCleanCommentAppearsOnDetailPage() throws Exception {
        Recipe r = seedRecipe("Comment Me Curry", owner);

        mvc.perform(post("/recipes/" + r.getId() + "/comment")
                .param("text", "Absolutely delicious!")
                .with(user(new CookifyUserDetails(visitor)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/recipes/" + r.getId()));

        mvc.perform(get("/recipes/" + r.getId()).with(user(new CookifyUserDetails(visitor))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Absolutely delicious!")))
           .andExpect(content().string(containsString("social7visitor")));
    }

    @Test
    void postNsfwCommentRedirectsWithErrorAndIsNotShown() throws Exception {
        Recipe r = seedRecipe("NSFW Test Curry", owner);

        mvc.perform(post("/recipes/" + r.getId() + "/comment")
                .param("text", "this is badword1 content")
                .with(user(new CookifyUserDetails(visitor)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/recipes/" + r.getId()))
           .andExpect(flash().attributeExists("commentError"));

        mvc.perform(get("/recipes/" + r.getId()).with(user(new CookifyUserDetails(visitor))))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("this is badword1 content"))));
    }

    @Test
    void postSubscribeMakesButtonShowUnsubscribe() throws Exception {
        Recipe r = seedRecipe("Subscribe Me Curry", owner);

        mvc.perform(post("/creators/" + owner.getId() + "/subscribe")
                .with(user(new CookifyUserDetails(visitor)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection());

        mvc.perform(get("/recipes/" + r.getId()).with(user(new CookifyUserDetails(visitor))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Unsubscribe")));
    }

    @Test
    void postSaveThenDetailShowsSavedToggleAndPersists() throws Exception {
        Recipe r = seedRecipe("Save Me Curry", owner);

        mvc.perform(post("/recipes/" + r.getId() + "/save")
                .with(user(new CookifyUserDetails(visitor)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/recipes/" + r.getId()));

        mvc.perform(get("/recipes/" + r.getId()).with(user(new CookifyUserDetails(visitor))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Saved")));
    }

    @Test
    void postUnsaveRemovesSavedState() throws Exception {
        Recipe r = seedRecipe("Unsave Me Curry", owner);

        mvc.perform(post("/recipes/" + r.getId() + "/save")
                .with(user(new CookifyUserDetails(visitor)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection());

        mvc.perform(post("/recipes/" + r.getId() + "/unsave")
                .with(user(new CookifyUserDetails(visitor)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/recipes/" + r.getId()));

        mvc.perform(get("/recipes/" + r.getId()).with(user(new CookifyUserDetails(visitor))))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("Saved ✓"))));
    }

    @Test
    void saveRequiresAuthentication() throws Exception {
        Recipe r = seedRecipe("Auth Required Curry", owner);

        mvc.perform(post("/recipes/" + r.getId() + "/save").with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void publicCreatorProfileShowsUploadedRecipesAndSubscribeButton() throws Exception {
        // Dedicated users (not the class-level owner/visitor) so this test's subscribe-button
        // assertion is never affected by subscriptions another test left behind in the shared,
        // non-rolled-back DB.
        User creator = seedUser("social7creatorpublic", "social7creatorpublic@example.com", "+917777777783");
        User fan = seedUser("social7fanpublic", "social7fanpublic@example.com", "+917777777784");
        seedRecipe("Public Profile Curry", creator);

        mvc.perform(get("/creators/" + creator.getUsername()))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Public Profile Curry")))
           .andExpect(content().string(containsString(creator.getUsername())));

        mvc.perform(get("/creators/" + creator.getUsername()).with(user(new CookifyUserDetails(fan))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Subscribe to Chef")));
    }

    @Test
    void creatorProfileShowsAvatarImageWhenProfileImagePathSet() throws Exception {
        User creator = seedUser("social7creatoravatar", "social7creatoravatar@example.com", "+917777777785");
        creator.setProfileImagePath("avatars/social7creatoravatar.png");
        users.save(creator);

        mvc.perform(get("/creators/" + creator.getUsername()))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("/uploads/")));
    }

    @Test
    void exploreVegFilterShowsOnlyVegCards() throws Exception {
        Recipe veg = RecipeFactory.create(true);
        veg.setRecipeName("Only Veg Card 7");
        veg.setRecipeCreatorId(owner.getId());
        recipes.save(veg);

        Recipe nonVeg = RecipeFactory.create(false);
        nonVeg.setRecipeName("Only NonVeg Card 7");
        nonVeg.setRecipeCreatorId(owner.getId());
        recipes.save(nonVeg);

        mvc.perform(get("/explore").param("veg", "true"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Only Veg Card 7")))
           .andExpect(content().string(not(containsString("Only NonVeg Card 7"))));
    }
}

package com.cookify.recipe;

import com.cookify.auth.CookifyUserDetails;
import com.cookify.category.RecipeCategory;
import com.cookify.category.RecipeCategoryRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecipeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired RecipeCategoryRepository categories;
    @Autowired PasswordEncoder encoder;

    static final byte[] PNG_1x1 = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    private User owner;
    private User other;

    @BeforeEach
    void seed() {
        owner = users.findByUsername("chefowner").orElseGet(() -> seedUser("chefowner", "chefowner@example.com", "+917777777771"));
        other = users.findByUsername("chefother").orElseGet(() -> seedUser("chefother", "chefother@example.com", "+917777777772"));
    }

    private User seedUser(String username, String email, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(u);
    }

    @Test
    void newFormRequiresAuthAndRendersHeading() throws Exception {
        mvc.perform(get("/recipes/new").with(user(new CookifyUserDetails(owner))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Add Title")));
    }

    @Test
    void createsRecipeAndRedirectsToDetail() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "pic.png", "image/png", PNG_1x1);

        MvcResult result = mvc.perform(multipart("/recipes")
                .file(image)
                .param("recipeName", "Paneer Tikka")
                .param("ingredient", "Paneer, spices")
                .param("recipeMethod", "Step 1\nStep 2")
                .param("veg", "true")
                .param("cost", "200")
                .param("speed", "3")
                .param("difficulty", "2")
                .param("cookingTime", "30")
                .param("calory", "400")
                .param("protin", "20")
                .with(user(new CookifyUserDetails(owner)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andReturn();

        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).matches("/recipes/\\d+");
        Long id = Long.valueOf(location.substring("/recipes/".length()));
        Recipe saved = recipes.findById(id).orElseThrow();
        assertThat(saved).isInstanceOf(VegRecipe.class);
        assertThat(saved.getRecipeName()).isEqualTo("Paneer Tikka");
        assertThat(saved.getImagePath()).endsWith(".png");
        assertThat(saved.getRecipeCreatorId()).isEqualTo(owner.getId());
    }

    @Test
    void editFormPrefillsForOwner() throws Exception {
        Recipe r = RecipeFactory.create(false);
        r.setRecipeName("Chicken Curry");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(get("/recipes/" + r.getId() + "/edit").with(user(new CookifyUserDetails(owner))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Chicken Curry")));
    }

    @Test
    void createWithBlankRecipeNameRerendersFormWithFieldErrorInsteadOf500() throws Exception {
        long before = recipes.count();

        mvc.perform(multipart("/recipes")
                .param("recipeName", "   ")
                .param("ingredient", "x")
                .param("recipeMethod", "Step 1")
                .param("veg", "true")
                .with(user(new CookifyUserDetails(owner)))
                .with(csrf()))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("ck-error")));

        assertThat(recipes.count()).isEqualTo(before);
    }

    @Test
    void updateWithBlankRecipeNameRerendersFormAndDoesNotPersist() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Valid Name Before Update");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(multipart("/recipes/" + r.getId())
                .param("recipeName", "")
                .param("ingredient", "x")
                .param("recipeMethod", "Step 1")
                .param("veg", "true")
                .with(user(new CookifyUserDetails(owner)))
                .with(csrf()))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("ck-error")));

        assertThat(recipes.findById(r.getId()).orElseThrow().getRecipeName())
            .isEqualTo("Valid Name Before Update");
    }

    @Test
    void editFormRendersDietaryTypeAsReadOnlyNotEditable() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Read Only Veg Curry");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(get("/recipes/" + r.getId() + "/edit").with(user(new CookifyUserDetails(owner))))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("type=\"radio\""))))
           .andExpect(content().string(containsString("Veg")));
    }

    @Test
    void newFormRendersEditableVegRadios() throws Exception {
        mvc.perform(get("/recipes/new").with(user(new CookifyUserDetails(owner))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("type=\"radio\"")));
    }

    @Test
    void editFormDeniedForNonOwner() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Owner Only Recipe");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(get("/recipes/" + r.getId() + "/edit").with(user(new CookifyUserDetails(other))))
           .andExpect(status().isForbidden());
    }

    @Test
    void updateByOwnerChangesFieldsAndRedirects() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Old Name");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(multipart("/recipes/" + r.getId())
                .param("recipeName", "New Name")
                .param("ingredient", "x")
                .param("recipeMethod", "Step 1")
                .param("veg", "true")
                .with(user(new CookifyUserDetails(owner)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/recipes/" + r.getId()));

        assertThat(recipes.findById(r.getId()).orElseThrow().getRecipeName()).isEqualTo("New Name");
    }

    @Test
    void updateCannotFlipImmutableVegTypeAndCategoryStaysInSync() throws Exception {
        MvcResult createResult = mvc.perform(multipart("/recipes")
                .param("recipeName", "Immutable Veg Curry")
                .param("ingredient", "Paneer")
                .param("recipeMethod", "Step 1")
                .param("veg", "true")
                .with(user(new CookifyUserDetails(owner)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andReturn();
        Long id = Long.valueOf(createResult.getResponse().getRedirectedUrl().substring("/recipes/".length()));

        // Attempt to flip the recipe to non-veg on update — the JPA subtype is immutable, so
        // this must be ignored both for the recipe's own type and for recipe_category.veg_nonveg.
        mvc.perform(multipart("/recipes/" + id)
                .param("recipeName", "Immutable Veg Curry")
                .param("ingredient", "Paneer")
                .param("recipeMethod", "Step 1")
                .param("veg", "false")
                .with(user(new CookifyUserDetails(owner)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection());

        Recipe reloaded = recipes.findById(id).orElseThrow();
        RecipeCategory reloadedCategory = categories.findById(id).orElseThrow();
        assertThat(reloaded.type()).isEqualTo(RecipeType.VEG);
        assertThat(reloadedCategory.isVegNonveg()).isTrue();
    }

    @Test
    void deleteByOwnerRedirectsToExplore() throws Exception {
        Recipe r = RecipeFactory.create(false);
        r.setRecipeName("To Delete");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);
        Long id = r.getId();

        mvc.perform(post("/recipes/" + id + "/delete")
                .with(user(new CookifyUserDetails(owner)))
                .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/explore"));

        assertThat(recipes.findById(id)).isEmpty();
    }

    @Test
    void deleteByNonOwnerForbidden() throws Exception {
        Recipe r = RecipeFactory.create(false);
        r.setRecipeName("Protected");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(post("/recipes/" + r.getId() + "/delete")
                .with(user(new CookifyUserDetails(other)))
                .with(csrf()))
           .andExpect(status().isForbidden());
    }

    @Test
    void detailPageShowsRecipeAndIncrementsViews() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Veg Pulao");
        r.setRecipeCreatorId(owner.getId());
        r.setRecipeMethod("Step 1\nStep 2");
        r = recipes.save(r);
        Long id = r.getId();

        mvc.perform(get("/recipes/" + id).with(user(new CookifyUserDetails(owner))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Veg Pulao")))
           .andExpect(content().string(containsString("Veg")));

        mvc.perform(get("/recipes/" + id).with(user(new CookifyUserDetails(owner))))
           .andExpect(status().isOk());

        assertThat(recipes.findById(id).orElseThrow().getViews()).isEqualTo(2L);
    }

    @Test
    void updateByNonOwnerRejectedAndWritesNoFile() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Original Name");
        r.setRecipeCreatorId(owner.getId());
        r.setImagePath(null);
        r = recipes.save(r);
        Long id = r.getId();

        MockMultipartFile image = new MockMultipartFile("image", "pic.png", "image/png", PNG_1x1);

        mvc.perform(multipart("/recipes/" + id)
                .file(image)
                .param("recipeName", "Hijacked Name")
                .param("ingredient", "x")
                .param("recipeMethod", "Step 1")
                .param("veg", "true")
                .with(user(new CookifyUserDetails(other)))
                .with(csrf()))
           .andExpect(status().isForbidden());

        Recipe unchanged = recipes.findById(id).orElseThrow();
        assertThat(unchanged.getRecipeName()).isEqualTo("Original Name");
        assertThat(unchanged.getImagePath()).isNull();
    }

    @Test
    void detailPageIsPublicForAnonymousUsers() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Anonymous Access Curry");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(get("/recipes/" + r.getId()))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Anonymous Access Curry")));
    }

    @Test
    void detailPageShowsEditAndDeleteForOwner() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Owner Action Buttons Curry");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(get("/recipes/" + r.getId()).with(user(new CookifyUserDetails(owner))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("ck-owner-actions")))
           .andExpect(content().string(containsString("Edit")))
           .andExpect(content().string(containsString("Delete")));
    }

    @Test
    void detailPageHidesEditAndDeleteForNonOwner() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Non Owner View Curry");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(get("/recipes/" + r.getId()).with(user(new CookifyUserDetails(other))))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("ck-owner-actions"))));
    }

    @Test
    void detailPageHidesEditAndDeleteForAnonymous() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Anonymous View Curry");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(get("/recipes/" + r.getId()))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("ck-owner-actions"))));
    }
}

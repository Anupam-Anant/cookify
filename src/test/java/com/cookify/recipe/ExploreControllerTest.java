package com.cookify.recipe;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExploreControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired RecipeCategoryRepository categories;
    @Autowired PasswordEncoder encoder;

    private User chef;

    @BeforeEach
    void seed() {
        chef = users.findByUsername("explorechef").orElseGet(() -> {
            User u = new User();
            u.setUsername("explorechef");
            u.setEmail("explorechef@example.com");
            u.setPhone("+917777777773");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });
    }

    @Test
    void explorePageIsPublicAndListsSeededRecipe() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Explore Me Curry");
        r.setRecipeCreatorId(chef.getId());
        recipes.save(r);

        mvc.perform(get("/explore"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Explore Recipes")))
           .andExpect(content().string(containsString("Explore Me Curry")));
    }

    @Test
    void recipeNewStillRequiresAuthWithoutLogin() throws Exception {
        mvc.perform(get("/recipes/new"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void searchInTitleOnlyMatchesByTitleNotByCreator() throws Exception {
        User creatorMatch = users.findByUsername("scopecreatormatch").orElseGet(() -> {
            User u = new User();
            u.setUsername("scopecreatormatch");
            u.setEmail("scopecreatormatch@example.com");
            u.setPhone("+917777777774");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });

        Recipe titleMatch = RecipeFactory.create(true);
        titleMatch.setRecipeName("Scopeword Title Curry");
        titleMatch.setRecipeCreatorId(chef.getId());
        recipes.save(titleMatch);

        Recipe creatorOnlyMatch = RecipeFactory.create(true);
        creatorOnlyMatch.setRecipeName("Totally Unrelated Dish");
        creatorOnlyMatch.setRecipeCreatorId(creatorMatch.getId());
        recipes.save(creatorOnlyMatch);

        // Only the Title checkbox is in scope: must find the title match...
        mvc.perform(get("/explore").param("query", "scopeword").param("searchInTitle", "true"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Scopeword Title Curry")));

        // ...but searching the creator-only term with Title-only scope must NOT match it.
        mvc.perform(get("/explore").param("query", "scopecreatormatch").param("searchInTitle", "true"))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("Totally Unrelated Dish"))));

        // Default scope (no checkboxes submitted at all) still matches by creator.
        mvc.perform(get("/explore").param("query", "scopecreatormatch"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Totally Unrelated Dish")));
    }

    @Test
    void exploreFormRendersThreeScopeCheckboxesCheckedByDefault() throws Exception {
        mvc.perform(get("/explore"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("name=\"searchInTitle\"")))
           .andExpect(content().string(containsString("name=\"searchInCreator\"")))
           .andExpect(content().string(containsString("name=\"searchInType\"")));

        String body = mvc.perform(get("/explore")).andReturn().getResponse().getContentAsString();
        // All three checkboxes carry a `checked` attribute when no filter has been submitted yet.
        long checkedScopeCheckboxes = java.util.regex.Pattern
            .compile("name=\"searchIn(Title|Creator|Type)\"[^>]*checked")
            .matcher(body)
            .results()
            .count();
        org.junit.jupiter.api.Assertions.assertEquals(3, checkedScopeCheckboxes);
    }

    @Test
    void recipeCardShowsDietaryColorBadgeDotIngredientRatingAndOpenButton() throws Exception {
        Recipe veg = RecipeFactory.create(true);
        veg.setRecipeName("Card Markup Veg Curry");
        veg.setIngredient("Rice, Lentils, Turmeric, Cumin, Coriander, Ginger, Garlic, Onion, Tomato, Chili, Salt, Oil");
        veg.setRecipeCreatorId(chef.getId());
        veg = recipes.save(veg);

        mvc.perform(get("/explore").param("query", "Card Markup Veg Curry").param("searchInTitle", "true"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("ck-card-veg")))
           .andExpect(content().string(containsString("ck-dot-veg")))
           .andExpect(content().string(containsString("ratings | Avg")))
           .andExpect(content().string(containsString("ck-open")))
           .andExpect(content().string(containsString("Open recipe")))
           .andExpect(content().string(containsString("/recipes/" + veg.getId())));
    }

    @Test
    void exploreFormRendersCookingTimeAndCalorieFilterInputs() throws Exception {
        mvc.perform(get("/explore"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("name=\"maxCookingTime\"")))
           .andExpect(content().string(containsString("name=\"maxCalories\"")));
    }

    @Test
    void maxCookingTimeFilterOnlyReturnsRecipesWithinLimit() throws Exception {
        Recipe quick = RecipeFactory.create(true);
        quick.setRecipeName("Quick Fifteen Minute Curry");
        quick.setRecipeCreatorId(chef.getId());
        quick = recipes.save(quick);
        RecipeCategory quickCategory = new RecipeCategory();
        quickCategory.setRecipeId(quick.getId());
        quickCategory.setVegNonveg(true);
        quickCategory.setCookingTime(15);
        categories.save(quickCategory);

        Recipe slow = RecipeFactory.create(true);
        slow.setRecipeName("Slow Forty Five Minute Curry");
        slow.setRecipeCreatorId(chef.getId());
        slow = recipes.save(slow);
        RecipeCategory slowCategory = new RecipeCategory();
        slowCategory.setRecipeId(slow.getId());
        slowCategory.setVegNonveg(true);
        slowCategory.setCookingTime(45);
        categories.save(slowCategory);

        mvc.perform(get("/explore").param("maxCookingTime", "20"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Quick Fifteen Minute Curry")))
           .andExpect(content().string(not(containsString("Slow Forty Five Minute Curry"))));
    }

    @Test
    void exploreCarouselHasArrowButtonsAndExternalScript() throws Exception {
        // Seeds its own recipe and scopes the query to it, rather than relying on GET /explore's
        // unfiltered list being non-empty — that list depends on what earlier tests (in whatever
        // order JUnit picks) have already seeded into the shared, non-rolled-back H2 instance.
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Carousel Arrows Curry");
        r.setRecipeCreatorId(chef.getId());
        recipes.save(r);

        mvc.perform(get("/explore").param("query", "Carousel Arrows Curry").param("searchInTitle", "true"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("ck-carousel-prev")))
           .andExpect(content().string(containsString("ck-carousel-next")))
           .andExpect(content().string(containsString("/js/explore-carousel.js")));
    }
}

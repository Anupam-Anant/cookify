package com.cookify.recipe;

import com.cookify.category.RecipeCategory;
import com.cookify.category.RecipeCategoryRepository;
import com.cookify.subscription.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeServiceTest {

    private RecipeRepository recipes;
    private RecipeCategoryRepository categories;
    private SubscriptionService subscriptionService;
    private RecipeService service;

    @BeforeEach
    void setUp() {
        recipes = mock(RecipeRepository.class);
        categories = mock(RecipeCategoryRepository.class);
        subscriptionService = mock(SubscriptionService.class);
        service = new RecipeService(recipes, categories, subscriptionService);
        when(recipes.save(any(Recipe.class))).thenAnswer(i -> {
            Recipe r = i.getArgument(0);
            if (r.getId() == null) {
                // simulate IDENTITY generation on first save
                try {
                    var field = Recipe.class.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(r, 1L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return r;
        });
        when(categories.save(any(RecipeCategory.class))).thenAnswer(i -> i.getArgument(0));
    }

    private RecipeForm form(boolean veg) {
        RecipeForm f = new RecipeForm();
        f.setRecipeName("Paneer Tikka");
        f.setIngredient("Paneer, spices");
        f.setRecipeMethod("Step 1\nStep 2");
        f.setVeg(veg);
        f.setCost(200);
        f.setSpeed(3);
        f.setDifficulty(2);
        f.setCookingTime(30);
        f.setCalory(400);
        f.setProtin(20);
        return f;
    }

    @Test
    void createReturnsVegRecipeAndPersistsCategory() {
        Recipe saved = service.create(form(true), 1L, "img.png", null);

        assertThat(saved).isInstanceOf(VegRecipe.class);
        assertThat(saved.getRecipeName()).isEqualTo("Paneer Tikka");
        assertThat(saved.getImagePath()).isEqualTo("img.png");
        assertThat(saved.getRecipeCreatorId()).isEqualTo(1L);

        verify(categories).save(argThat(c ->
            c.getRecipeId().equals(saved.getId())
                && c.isVegNonveg()
                && c.getCookingTime().equals(30)
                && c.getCalory().equals(400)
                && c.getProtin().equals(20)));
        verify(subscriptionService).notifyNewRecipe(1L, saved);
    }

    @Test
    void createReturnsNonVegRecipeWhenNotVeg() {
        Recipe saved = service.create(form(false), 1L, null, "clip.mp4");
        assertThat(saved).isInstanceOf(NonVegRecipe.class);
        assertThat(saved.getRecipeVideo()).isEqualTo("clip.mp4");
    }

    @Test
    void getMissingThrowsRecipeNotFound() {
        when(recipes.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L))
            .isInstanceOf(RecipeNotFoundException.class);
    }

    @Test
    void updateByNonOwnerThrowsAccessDenied() {
        NonVegRecipe existing = new NonVegRecipe();
        existing.setRecipeCreatorId(1L);
        when(recipes.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(5L, form(false), 2L, null, null))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateKeepsCategoryVegNonvegInSyncWithImmutableRecipeTypeEvenIfFormDisagrees() throws Exception {
        VegRecipe existing = new VegRecipe();
        existing.setRecipeCreatorId(1L);
        var idField = Recipe.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(existing, 10L);

        when(recipes.findById(10L)).thenReturn(Optional.of(existing));
        when(categories.findById(10L)).thenReturn(Optional.empty());

        // Attempts to flip the recipe to non-veg via the form; the underlying JPA subtype
        // (VegRecipe) is immutable, so this must be ignored rather than desyncing the category.
        RecipeForm formAttemptingNonVeg = form(false);

        service.update(10L, formAttemptingNonVeg, 1L, null, null);

        verify(categories).save(argThat(c -> c.getRecipeId().equals(10L) && c.isVegNonveg()));
    }

    @Test
    void incrementViewsAndGetRaisesCount() {
        NonVegRecipe existing = new NonVegRecipe();
        existing.setViews(5L);
        when(recipes.findById(7L)).thenReturn(Optional.of(existing));

        Recipe result = service.incrementViewsAndGet(7L);

        assertThat(result.getViews()).isEqualTo(6L);
        verify(recipes).save(existing);
    }
}

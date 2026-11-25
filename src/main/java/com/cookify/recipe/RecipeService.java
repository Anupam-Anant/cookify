package com.cookify.recipe;

import com.cookify.category.RecipeCategory;
import com.cookify.category.RecipeCategoryRepository;
import com.cookify.subscription.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeRepository recipes;
    private final RecipeCategoryRepository categories;
    private final SubscriptionService subscriptionService;

    public RecipeService(RecipeRepository recipes, RecipeCategoryRepository categories,
                          SubscriptionService subscriptionService) {
        this.recipes = recipes;
        this.categories = categories;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public Recipe create(RecipeForm form, Long creatorId, String imagePath, String videoPath) {
        Recipe recipe = RecipeFactory.create(form.isVeg());
        applyForm(recipe, form);
        recipe.setRecipeCreatorId(creatorId);
        if (imagePath != null) {
            recipe.setImagePath(imagePath);
        }
        if (videoPath != null) {
            recipe.setRecipeVideo(videoPath);
        }
        Recipe saved = recipes.save(recipe);
        saveCategory(saved, form);
        subscriptionService.notifyNewRecipe(creatorId, saved);
        log.info("Recipe created recipeId={} creatorId={}", saved.getId(), creatorId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Recipe get(Long id) {
        return recipes.findById(id).orElseThrow(() -> new RecipeNotFoundException(id));
    }

    /**
     * Loads the recipe and throws if {@code actingUserId} is not its creator.
     * Callers that accept file uploads (e.g. the update controller) must call
     * this BEFORE storing any file, so a non-owner's request never reaches
     * disk before being rejected.
     */
    public void assertOwner(Long id, Long actingUserId) {
        requireOwner(get(id), actingUserId);
    }

    @Transactional
    public Recipe update(Long id, RecipeForm form, Long actingUserId, String imagePath, String videoPath) {
        Recipe recipe = get(id);
        requireOwner(recipe, actingUserId);
        applyForm(recipe, form);
        if (imagePath != null) {
            recipe.setImagePath(imagePath);
        }
        if (videoPath != null) {
            recipe.setRecipeVideo(videoPath);
        }
        Recipe saved = recipes.save(recipe);
        saveCategory(saved, form);
        return saved;
    }

    @Transactional
    public void delete(Long id, Long actingUserId) {
        Recipe recipe = get(id);
        requireOwner(recipe, actingUserId);
        categories.deleteById(id);
        recipes.delete(recipe);
        log.info("Recipe deleted recipeId={} creatorId={}", id, recipe.getRecipeCreatorId());
    }

    @Transactional
    public Recipe incrementViewsAndGet(Long id) {
        Recipe recipe = get(id);
        recipe.setViews(recipe.getViews() + 1);
        return recipes.save(recipe);
    }

    private void requireOwner(Recipe recipe, Long actingUserId) {
        if (!recipe.getRecipeCreatorId().equals(actingUserId)) {
            throw new AccessDeniedException("Only the recipe owner may perform this action.");
        }
    }

    private void applyForm(Recipe recipe, RecipeForm form) {
        recipe.setRecipeName(form.getRecipeName());
        recipe.setIngredient(form.getIngredient());
        recipe.setRecipeMethod(form.getRecipeMethod());
        recipe.setCost(form.getCost());
        recipe.setSpeed(form.getSpeed());
        recipe.setDifficulty(form.getDifficulty());
    }

    /**
     * The dietary type is the immutable JPA subtype ({@code recipe_type} discriminator) — it
     * cannot change after a recipe is created. {@code recipe_category.veg_nonveg} must therefore
     * ALWAYS be derived from {@link Recipe#type()}, never from the (mutable, user-supplied)
     * {@code form.isVeg()} — on both create and update — so the two can never diverge.
     */
    private void saveCategory(Recipe recipe, RecipeForm form) {
        Long recipeId = recipe.getId();
        RecipeCategory category = categories.findById(recipeId).orElseGet(RecipeCategory::new);
        category.setRecipeId(recipeId);
        category.setVegNonveg(recipe.type() == RecipeType.VEG);
        category.setCookingTime(form.getCookingTime());
        category.setCalory(form.getCalory());
        category.setProtin(form.getProtin());
        categories.save(category);
    }
}

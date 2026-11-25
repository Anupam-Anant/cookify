package com.cookify.saved;

import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Save/unsave business logic on top of {@link SavedRecipe}: lets a user bookmark any recipe for
 * later, idempotently, and lists their saved recipes (shown on the profile page).
 */
@Service
public class SavedRecipeService {

    private final SavedRecipeRepository savedRecipeRepository;
    private final RecipeRepository recipeRepository;

    public SavedRecipeService(SavedRecipeRepository savedRecipeRepository, RecipeRepository recipeRepository) {
        this.savedRecipeRepository = savedRecipeRepository;
        this.recipeRepository = recipeRepository;
    }

    @Transactional
    public void save(Long userId, Long recipeId) {
        if (savedRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            return;
        }
        SavedRecipe saved = new SavedRecipe();
        saved.setUserId(userId);
        saved.setRecipeId(recipeId);
        savedRecipeRepository.save(saved);
    }

    @Transactional
    public void unsave(Long userId, Long recipeId) {
        savedRecipeRepository.deleteByUserIdAndRecipeId(userId, recipeId);
    }

    public boolean isSaved(Long userId, Long recipeId) {
        return savedRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }

    public List<Recipe> savedRecipes(Long userId) {
        return savedRecipeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(SavedRecipe::getRecipeId)
            .map(recipeRepository::findById)
            .flatMap(Optional::stream)
            .toList();
    }
}

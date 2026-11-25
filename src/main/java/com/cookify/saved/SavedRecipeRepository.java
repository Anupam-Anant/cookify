package com.cookify.saved;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, Long> {
    Optional<SavedRecipe> findByUserIdAndRecipeId(Long userId, Long recipeId);
    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);
    List<SavedRecipe> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);

    // Needed by ModerationService.banAndPurge to remove bookmarks before a purged recipe is
    // deleted (FK saved_recipe.recipe_id is NOT NULL / no cascade — V4 migration), and to
    // remove the banned user's own bookmarks of other people's recipes.
    void deleteByRecipeIdIn(List<Long> recipeIds);
    void deleteByUserId(Long userId);
}

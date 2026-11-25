package com.cookify.rating;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecipeRatingRepository extends JpaRepository<RecipeRating, Long> {
    Optional<RecipeRating> findByRecipeIdAndUserId(Long recipeId, Long userId);
    List<RecipeRating> findByRecipeId(Long recipeId);
    List<RecipeRating> findByRecipeIdIn(Collection<Long> recipeIds);

    // Needed by ModerationService.banAndPurge to remove a deleted recipe's rating rows (FK
    // recipe_rating.recipe_id is NOT NULL / no cascade) and a banned user's own rating/comment
    // rows across all recipes.
    void deleteByRecipeId(Long recipeId);
    void deleteByUserId(Long userId);
}

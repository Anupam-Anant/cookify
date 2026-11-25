package com.cookify.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {
    List<Recipe> findByRecipeCreatorId(Long recipeCreatorId);
    List<Recipe> findAllByOrderByCreatedAtDesc();
}

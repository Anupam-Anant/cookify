package com.cookify.saved;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A bookmark linking a user to a recipe they want to find again later. Table
 * {@code saved_recipe} (V4 migration): unique per ({@code user_id}, {@code recipe_id}).
 */
@Entity
@Table(name = "saved_recipe")
public class SavedRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long v) { this.recipeId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

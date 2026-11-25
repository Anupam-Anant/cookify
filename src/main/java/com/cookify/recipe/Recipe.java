package com.cookify.recipe;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipe")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "recipe_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id") private Long id;
    @Column(name = "recipe_name", nullable = false) private String recipeName;
    private String ingredient;
    @Column(name = "recipe_method") private String recipeMethod;
    @Column(name = "recipe_video") private String recipeVideo;
    @Column(name = "image_path") private String imagePath;
    @Column(name = "recipe_creator_id", nullable = false) private Long recipeCreatorId;
    @Column(nullable = false) private long views = 0;
    private Integer cost;
    private Integer speed;
    private Integer difficulty;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public abstract String dietaryLabel();
    public abstract RecipeType type();
    // getters/setters for all fields (generate)
    public Long getId() { return id; }
    public String getRecipeName() { return recipeName; }
    public void setRecipeName(String v){ recipeName=v; }
    public String getIngredient(){ return ingredient; }
    public void setIngredient(String v){ ingredient=v; }
    public String getRecipeMethod(){ return recipeMethod; }
    public void setRecipeMethod(String v){ recipeMethod=v; }
    public String getRecipeVideo(){ return recipeVideo; }
    public void setRecipeVideo(String v){ recipeVideo=v; }
    public String getImagePath(){ return imagePath; }
    public void setImagePath(String v){ imagePath=v; }
    public Long getRecipeCreatorId(){ return recipeCreatorId; }
    public void setRecipeCreatorId(Long v){ recipeCreatorId=v; }
    public long getViews(){ return views; }
    public void setViews(long v){ views=v; }
    public Integer getCost(){ return cost; }
    public void setCost(Integer v){ cost=v; }
    public Integer getSpeed(){ return speed; }
    public void setSpeed(Integer v){ speed=v; }
    public Integer getDifficulty(){ return difficulty; }
    public void setDifficulty(Integer v){ difficulty=v; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}

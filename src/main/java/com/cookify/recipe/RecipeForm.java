package com.cookify.recipe;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class RecipeForm {

    @NotBlank(message = "Recipe name is required")
    private String recipeName;

    @Size(max = 5000, message = "Ingredients must be at most 5000 characters")
    private String ingredient;

    @Size(max = 5000, message = "Method must be at most 5000 characters")
    private String recipeMethod;

    private boolean veg;

    @PositiveOrZero(message = "Cost must be zero or positive")
    private Integer cost;

    @Min(value = 1, message = "Speed must be between 1 and 5")
    @Max(value = 5, message = "Speed must be between 1 and 5")
    private Integer speed;

    @Min(value = 1, message = "Difficulty must be between 1 and 5")
    @Max(value = 5, message = "Difficulty must be between 1 and 5")
    private Integer difficulty;

    @PositiveOrZero(message = "Cooking time must be zero or positive")
    private Integer cookingTime;

    @PositiveOrZero(message = "Calories must be zero or positive")
    private Integer calory;

    private Integer protin;

    public String getRecipeName() { return recipeName; }
    public void setRecipeName(String v) { this.recipeName = v; }
    public String getIngredient() { return ingredient; }
    public void setIngredient(String v) { this.ingredient = v; }
    public String getRecipeMethod() { return recipeMethod; }
    public void setRecipeMethod(String v) { this.recipeMethod = v; }
    public boolean isVeg() { return veg; }
    public void setVeg(boolean v) { this.veg = v; }
    public Integer getCost() { return cost; }
    public void setCost(Integer v) { this.cost = v; }
    public Integer getSpeed() { return speed; }
    public void setSpeed(Integer v) { this.speed = v; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer v) { this.difficulty = v; }
    public Integer getCookingTime() { return cookingTime; }
    public void setCookingTime(Integer v) { this.cookingTime = v; }
    public Integer getCalory() { return calory; }
    public void setCalory(Integer v) { this.calory = v; }
    public Integer getProtin() { return protin; }
    public void setProtin(Integer v) { this.protin = v; }
}

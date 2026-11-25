package com.cookify.category;

import jakarta.persistence.*;

@Entity
@Table(name = "recipe_category")
public class RecipeCategory {

    @Id
    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "veg_nonveg")
    private boolean vegNonveg;

    @Column(name = "cooking_time")
    private Integer cookingTime;

    private Integer calory;

    private Integer protin;

    @Column(name = "preference_id")
    private Long preferenceId;

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long v) { this.recipeId = v; }
    public boolean isVegNonveg() { return vegNonveg; }
    public void setVegNonveg(boolean v) { this.vegNonveg = v; }
    public Integer getCookingTime() { return cookingTime; }
    public void setCookingTime(Integer v) { this.cookingTime = v; }
    public Integer getCalory() { return calory; }
    public void setCalory(Integer v) { this.calory = v; }
    public Integer getProtin() { return protin; }
    public void setProtin(Integer v) { this.protin = v; }
    public Long getPreferenceId() { return preferenceId; }
    public void setPreferenceId(Long v) { this.preferenceId = v; }
}

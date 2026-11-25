package com.cookify.recipe;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("NONVEG")
public class NonVegRecipe extends Recipe {
    public String dietaryLabel(){ return "Non-Veg"; }
    public RecipeType type(){ return RecipeType.NONVEG; }
}

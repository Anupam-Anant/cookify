package com.cookify.recipe;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("VEG")
public class VegRecipe extends Recipe {
    public String dietaryLabel(){ return "Veg"; }
    public RecipeType type(){ return RecipeType.VEG; }
}

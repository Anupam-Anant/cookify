package com.cookify.recipe;

public final class RecipeFactory {
    private RecipeFactory(){}
    public static Recipe create(boolean veg){ return veg ? new VegRecipe() : new NonVegRecipe(); }
}

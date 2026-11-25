package com.cookify.recipe;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeHierarchyTest {
    @Test void factoryPicksSubclassAndPolymorphicLabel() {
        Recipe veg = RecipeFactory.create(true);
        Recipe non = RecipeFactory.create(false);
        assertThat(veg).isInstanceOf(VegRecipe.class);
        assertThat(non).isInstanceOf(NonVegRecipe.class);
        assertThat(veg.dietaryLabel()).isEqualTo("Veg");
        assertThat(non.dietaryLabel()).isEqualTo("Non-Veg");
        assertThat(veg.type()).isEqualTo(RecipeType.VEG);
    }
}

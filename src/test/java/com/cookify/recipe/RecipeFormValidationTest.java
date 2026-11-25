package com.cookify.recipe;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure bean-validation tests for {@link RecipeForm}'s {@code jakarta.validation} constraints —
 * no Spring context needed. {@link RecipeControllerTest} covers the controller wiring (re-render
 * with field errors instead of a 500) on top of these.
 */
class RecipeFormValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private RecipeForm validForm() {
        RecipeForm form = new RecipeForm();
        form.setRecipeName("Paneer Tikka");
        form.setIngredient("Paneer, spices");
        form.setRecipeMethod("Step 1\nStep 2");
        form.setVeg(true);
        form.setCost(200);
        form.setSpeed(3);
        form.setDifficulty(2);
        form.setCookingTime(30);
        form.setCalory(400);
        return form;
    }

    @Test
    void validFormHasNoViolations() {
        assertThat(validator.validate(validForm())).isEmpty();
    }

    @Test
    void blankRecipeNameIsRejected() {
        RecipeForm form = validForm();
        form.setRecipeName("  ");
        Set<ConstraintViolation<RecipeForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("recipeName"));
    }

    @Test
    void overlongIngredientIsRejected() {
        RecipeForm form = validForm();
        form.setIngredient("a".repeat(5001));
        assertThat(validator.validate(form))
            .anyMatch(v -> v.getPropertyPath().toString().equals("ingredient"));
    }

    @Test
    void overlongMethodIsRejected() {
        RecipeForm form = validForm();
        form.setRecipeMethod("a".repeat(5001));
        assertThat(validator.validate(form))
            .anyMatch(v -> v.getPropertyPath().toString().equals("recipeMethod"));
    }

    @Test
    void negativeCostIsRejected() {
        RecipeForm form = validForm();
        form.setCost(-1);
        assertThat(validator.validate(form))
            .anyMatch(v -> v.getPropertyPath().toString().equals("cost"));
    }

    @Test
    void speedOutsideOneToFiveIsRejected() {
        RecipeForm form = validForm();
        form.setSpeed(6);
        assertThat(validator.validate(form))
            .anyMatch(v -> v.getPropertyPath().toString().equals("speed"));

        form.setSpeed(0);
        assertThat(validator.validate(form))
            .anyMatch(v -> v.getPropertyPath().toString().equals("speed"));
    }

    @Test
    void difficultyOutsideOneToFiveIsRejected() {
        RecipeForm form = validForm();
        form.setDifficulty(6);
        assertThat(validator.validate(form))
            .anyMatch(v -> v.getPropertyPath().toString().equals("difficulty"));
    }

    @Test
    void negativeCookingTimeIsRejected() {
        RecipeForm form = validForm();
        form.setCookingTime(-5);
        assertThat(validator.validate(form))
            .anyMatch(v -> v.getPropertyPath().toString().equals("cookingTime"));
    }

    @Test
    void negativeCaloryIsRejected() {
        RecipeForm form = validForm();
        form.setCalory(-5);
        assertThat(validator.validate(form))
            .anyMatch(v -> v.getPropertyPath().toString().equals("calory"));
    }

    @Test
    void nullOptionalNumericFieldsAreAccepted() {
        RecipeForm form = validForm();
        form.setCost(null);
        form.setSpeed(null);
        form.setDifficulty(null);
        form.setCookingTime(null);
        form.setCalory(null);
        assertThat(validator.validate(form)).isEmpty();
    }
}

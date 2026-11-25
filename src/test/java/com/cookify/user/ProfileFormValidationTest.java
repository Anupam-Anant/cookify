package com.cookify.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure bean-validation tests for {@link ProfileForm}'s {@code jakarta.validation} constraints —
 * no Spring context needed. {@link ProfileControllerTest} covers the controller wiring on top.
 */
class ProfileFormValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private ProfileForm validForm() {
        ProfileForm form = new ProfileForm();
        form.setNameFirst("Ramgopal");
        form.setNameLast("Patidar");
        form.setAge(30);
        form.setGender("Other");
        form.setBio("I love cooking.");
        return form;
    }

    @Test
    void validFormHasNoViolations() {
        assertThat(validator.validate(validForm())).isEmpty();
    }

    @Test
    void overlongBioIsRejected() {
        ProfileForm form = validForm();
        form.setBio("a".repeat(501));
        Set<ConstraintViolation<ProfileForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("bio"));
    }

    @Test
    void bioAtExactLimitIsAccepted() {
        ProfileForm form = validForm();
        form.setBio("a".repeat(500));
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void ageOutsideOneToOneTwentyIsRejected() {
        ProfileForm form = validForm();
        form.setAge(0);
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("age"));

        form.setAge(121);
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("age"));
    }

    @Test
    void nullAgeIsAccepted() {
        ProfileForm form = validForm();
        form.setAge(null);
        assertThat(validator.validate(form)).isEmpty();
    }
}

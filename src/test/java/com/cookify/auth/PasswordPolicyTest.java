package com.cookify.auth;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsCompliantPassword() {
        assertThat(policy.isValid("Tasty-Recipe123")).isTrue();
        assertThat(policy.validate("Tasty-Recipe123")).isEmpty();
    }

    @Test
    void rejectsTooShort() {
        assertThat(policy.isValid("Short12")).isFalse();
        assertThat(policy.validate("Short12"))
            .anyMatch(m -> m.toLowerCase().contains("9"));
    }

    @Test
    void rejectsSpaces() {
        assertThat(policy.isValid("has space 123")).isFalse();
        assertThat(policy.validate("has space 123"))
            .anyMatch(m -> m.toLowerCase().contains("space"));
    }

    @Test
    void rejectsRestrictedSymbols() {
        assertThat(policy.isValid("bad<script>x")).isFalse();
    }
}

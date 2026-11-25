package com.cookify.auth;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordPolicy {

    private static final int MIN_LENGTH = 9;
    private static final String RESTRICTED = "<>\"'\\/`";

    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Password must be at least 9 characters long.");
        }
        if (password != null && password.chars().anyMatch(Character::isWhitespace)) {
            errors.add("Password must not contain spaces.");
        }
        if (password != null && password.chars().anyMatch(c -> RESTRICTED.indexOf(c) >= 0)) {
            errors.add("Password must not contain restricted symbols (< > \" ' \\ / `).");
        }
        return errors;
    }

    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}

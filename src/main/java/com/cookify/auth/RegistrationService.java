package com.cookify.auth;

import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final PasswordPolicy policy;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository users, PasswordPolicy policy, PasswordEncoder encoder) {
        this.users = users;
        this.policy = policy;
        this.encoder = encoder;
    }

    @Transactional
    public User register(RegistrationForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new RegistrationException("confirmPassword", "Passwords do not match.");
        }
        List<String> pwErrors = policy.validate(form.getPassword());
        if (!pwErrors.isEmpty()) {
            throw new RegistrationException("password", String.join(" ", pwErrors));
        }
        if (users.existsByUsername(form.getUsername())) {
            throw new RegistrationException("username", "This username is taken, try again.");
        }
        if (users.existsByEmail(form.getEmail())) {
            throw new RegistrationException("email", "Email already in use.");
        }
        if (users.existsByPhone(form.getPhone())) {
            throw new RegistrationException("phone", "Phone number already in use.");
        }
        User user = new User();
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setPhone(form.getPhone());
        user.setPasswordHash(encoder.encode(form.getPassword()));
        user.setRole(Role.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(user);
    }
}

package com.cookify.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.cookify.user.User;
import com.cookify.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    private UserRepository users;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        service = new RegistrationService(users, new PasswordPolicy(), new BCryptPasswordEncoder());
        when(users.existsByUsername(anyString())).thenReturn(false);
        when(users.existsByEmail(anyString())).thenReturn(false);
        when(users.existsByPhone(anyString())).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    private RegistrationForm form() {
        RegistrationForm f = new RegistrationForm();
        f.setUsername("aryan");
        f.setEmail("aryan@example.com");
        f.setPhone("+919999999999");
        f.setPassword("Tasty-Recipe123");
        f.setConfirmPassword("Tasty-Recipe123");
        return f;
    }

    @Test
    void registersValidUserWithHashedPassword() {
        User saved = service.register(form());
        assertThat(saved.getUsername()).isEqualTo("aryan");
        assertThat(saved.getPasswordHash()).isNotEqualTo("Tasty-Recipe123");
        assertThat(saved.getPasswordHash()).startsWith("$2");
        verify(users).save(any(User.class));
    }

    @Test
    void rejectsDuplicateUsername() {
        when(users.existsByUsername("aryan")).thenReturn(true);
        assertThatThrownBy(() -> service.register(form()))
            .isInstanceOf(RegistrationException.class)
            .extracting(e -> ((RegistrationException) e).getField())
            .isEqualTo("username");
    }

    @Test
    void rejectsWeakPassword() {
        RegistrationForm f = form();
        f.setPassword("short"); f.setConfirmPassword("short");
        assertThatThrownBy(() -> service.register(f))
            .isInstanceOf(RegistrationException.class)
            .extracting(e -> ((RegistrationException) e).getField())
            .isEqualTo("password");
    }

    @Test
    void rejectsPasswordMismatch() {
        RegistrationForm f = form();
        f.setConfirmPassword("Different-Pass123");
        assertThatThrownBy(() -> service.register(f))
            .isInstanceOf(RegistrationException.class)
            .extracting(e -> ((RegistrationException) e).getField())
            .isEqualTo("confirmPassword");
    }
}

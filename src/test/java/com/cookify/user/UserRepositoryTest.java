package com.cookify.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired UserRepository users;

    private User newUser() {
        User u = new User();
        u.setUsername("aryan");
        u.setEmail("aryan@example.com");
        u.setPhone("+919999999999");
        u.setPasswordHash("$2a$dummy");
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return u;
    }

    @Test
    void savesAndLooksUpByUsernameEmailPhone() {
        users.save(newUser());

        assertThat(users.findByUsername("aryan")).isPresent();
        assertThat(users.findByEmail("aryan@example.com")).isPresent();
        assertThat(users.findByPhone("+919999999999")).isPresent();

        Optional<User> byAny = users.findByUsernameOrEmailOrPhone(
            "aryan@example.com", "aryan@example.com", "aryan@example.com");
        assertThat(byAny).isPresent();
    }

    @Test
    void enforcesUniqueUsernameAndEmail() {
        users.save(newUser());
        assertThat(users.existsByUsername("aryan")).isTrue();
        assertThat(users.existsByEmail("aryan@example.com")).isTrue();
        assertThat(users.existsByUsername("nobody")).isFalse();
    }
}

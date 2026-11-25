package com.cookify.auth;

import com.cookify.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

@SpringBootTest
@AutoConfigureMockMvc
class RememberMeTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void seed() {
        if (users.findByUsername("rememberme").isEmpty()) {
            User u = new User();
            u.setUsername("rememberme");
            u.setEmail("rm@example.com");
            u.setPhone("+914444444444");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            users.save(u);
        }
    }

    @Test
    void setsRememberMeCookie() throws Exception {
        mvc.perform(post("/login").with(csrf())
                .param("username", "rememberme")
                .param("password", "Tasty-Recipe123")
                .param("remember-me", "on"))
           .andExpect(cookie().exists("remember-me"));
    }
}

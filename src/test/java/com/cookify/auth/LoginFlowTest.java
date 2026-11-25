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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LoginFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void seed() {
        if (users.findByUsername("loginuser").isEmpty()) {
            User u = new User();
            u.setUsername("loginuser");
            u.setEmail("login@example.com");
            u.setPhone("+916666666666");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setTwoFactorEnabled(false);
            users.save(u);
        }
    }

    @Test
    void loginByUsernameSucceeds() throws Exception {
        mvc.perform(post("/login").with(csrf())
                .param("username", "loginuser")
                .param("password", "Tasty-Recipe123"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/explore"));
    }

    @Test
    void loginByEmailSucceeds() throws Exception {
        mvc.perform(post("/login").with(csrf())
                .param("username", "login@example.com")
                .param("password", "Tasty-Recipe123"))
           .andExpect(redirectedUrl("/explore"));
    }

    @Test
    void wrongPasswordFails() throws Exception {
        mvc.perform(post("/login").with(csrf())
                .param("username", "loginuser")
                .param("password", "wrong-password"))
           .andExpect(redirectedUrl("/login?error"));
    }
}

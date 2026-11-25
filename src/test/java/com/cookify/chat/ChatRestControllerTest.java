package com.cookify.chat;

import com.cookify.auth.CookifyUserDetails;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest + MockMvc against the real (shared, non-rolled-back) H2 "cookify" instance —
// same pattern as RecipeSocialControllerTest.
@SpringBootTest
@AutoConfigureMockMvc
class ChatRestControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    private User alice;
    private User bob;

    @BeforeEach
    void seed() {
        alice = seedUser("chatrest1alice", "chatrest1alice@example.com", "+917777771001");
        bob = seedUser("chatrest1bob", "chatrest1bob@example.com", "+917777771002");
    }

    private User seedUser(String username, String email, String phone) {
        return users.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPhone(phone);
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });
    }

    @Test
    void postThenGetReturnsTheMessage() throws Exception {
        mvc.perform(post("/chat/" + bob.getId() + "/messages")
                .param("body", "Hello Bob!")
                .with(user(new CookifyUserDetails(alice)))
                .with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.body").value("Hello Bob!"))
           .andExpect(jsonPath("$.senderId").value(alice.getId()))
           .andExpect(jsonPath("$.recipientId").value(bob.getId()));

        mvc.perform(get("/chat/" + bob.getId() + "/messages")
                .with(user(new CookifyUserDetails(alice))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(1)))
           .andExpect(jsonPath("$[0].body").value("Hello Bob!"));

        // Bob sees the same conversation from the other side.
        mvc.perform(get("/chat/" + alice.getId() + "/messages")
                .with(user(new CookifyUserDetails(bob))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(1)))
           .andExpect(jsonPath("$[0].body").value("Hello Bob!"));
    }

    @Test
    void unauthenticatedGetIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/chat/" + bob.getId() + "/messages"))
           .andExpect(status().is3xxRedirection());
    }
}

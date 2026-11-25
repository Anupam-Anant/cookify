package com.cookify.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SignupControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void getRendersForm() throws Exception {
        mvc.perform(get("/signup"))
           .andExpect(status().isOk())
           .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign Up")));
    }

    @Test
    void postValidRegistersAndRedirects() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                .param("username", "newcook")
                .param("email", "newcook@example.com")
                .param("phone", "+918888888888")
                .param("password", "Tasty-Recipe123")
                .param("confirmPassword", "Tasty-Recipe123"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void postDuplicateUsernameShowsError() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                .param("username", "dupuser")
                .param("email", "dup1@example.com")
                .param("phone", "+917777777777")
                .param("password", "Tasty-Recipe123")
                .param("confirmPassword", "Tasty-Recipe123"))
           .andExpect(status().is3xxRedirection());
        // second registration with same username
        mvc.perform(post("/signup").with(csrf())
                .param("username", "dupuser")
                .param("email", "dup2@example.com")
                .param("phone", "+917777777000")
                .param("password", "Tasty-Recipe123")
                .param("confirmPassword", "Tasty-Recipe123"))
           .andExpect(status().isOk())
           .andExpect(content().string(org.hamcrest.Matchers.containsString("taken")));
    }
}

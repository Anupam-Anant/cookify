package com.cookify.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Live-boot validation found that GET /login returned a 302 jsessionid
 * URL-rewrite redirect instead of cleanly rendering the login form (no
 * GET /login handler existed — only the POST login processed by Spring
 * Security). This verifies the login page renders directly, unauthenticated,
 * with status 200 and the actual form markup.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginPageControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void getLoginRendersFormDirectly() throws Exception {
        mvc.perform(get("/login"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Confirm Login")))
           .andExpect(content().string(containsString("name=\"username\"")));
    }
}

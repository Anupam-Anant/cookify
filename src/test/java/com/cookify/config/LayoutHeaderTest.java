package com.cookify.config;

import com.cookify.auth.CookifyUserDetails;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the header fixes from the UI-match brief: the {@code langSwitcher} fragment previously
 * rendered twice (once via {@code th:replace}, once as its own literal definition further down
 * the body) — this guards it renders exactly once — and the new logged-in-username / Login-SignUp
 * header area.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LayoutHeaderTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    private static int countOccurrences(String haystack, String needle) {
        Pattern pattern = Pattern.compile(Pattern.quote(needle));
        return (int) pattern.matcher(haystack).results().count();
    }

    @Test
    void langSwitcherRendersExactlyOnceOnLayoutBackedPages() throws Exception {
        MockHttpServletResponse home = mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andReturn().getResponse();
        assertEquals(1, countOccurrences(home.getContentAsString(), "id=\"ck-lang-select\""));

        MockHttpServletResponse explore = mvc.perform(get("/explore"))
            .andExpect(status().isOk())
            .andReturn().getResponse();
        assertEquals(1, countOccurrences(explore.getContentAsString(), "id=\"ck-lang-select\""));
    }

    @Test
    void loggedOutHeaderShowsLoginAndSignUpLinksNotUsernameOrLogout() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("ck-auth-link")))
           .andExpect(content().string(containsString("Login")))
           .andExpect(content().string(containsString("Sign Up")))
           .andExpect(content().string(not(containsString("ck-logout"))));
    }

    @Test
    void loggedInHeaderShowsUsernameAndLogoutFormNotLoginSignUp() throws Exception {
        User seeded = users.findByUsername("headeruser").orElseGet(() -> {
            User u = new User();
            u.setUsername("headeruser");
            u.setEmail("headeruser@example.com");
            u.setPhone("+919999999900");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });

        mvc.perform(get("/explore").with(user(new CookifyUserDetails(seeded))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("headeruser")))
           .andExpect(content().string(containsString("ck-logout")))
           .andExpect(content().string(containsString("action=\"/logout\"")))
           .andExpect(content().string(not(containsString("ck-auth-link"))));
    }
}

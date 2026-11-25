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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

// Uses dedicated seeded users ("lockoutuser", "unaffecteduser") not referenced by any other
// test, so this test's deliberate 5x failed-login lockout can never bleed into another test's
// shared user. LoginAttemptService is an in-memory singleton bean (state lives for the cached
// Spring context's lifetime), so these usernames must stay unique to this class.
@SpringBootTest
@AutoConfigureMockMvc
class LoginLockoutTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void seed() {
        seedUser("lockoutuser", "lockout@example.com", "+919191919191");
        seedUser("unaffecteduser", "unaffected@example.com", "+919292929292");
        seedUser("emaillockoutuser", "emaillockout@example.com", "+919393939393");
    }

    private void seedUser(String username, String email, String phone) {
        if (users.findByUsername(username).isEmpty()) {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPhone(phone);
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setTwoFactorEnabled(false);
            users.save(u);
        }
    }

    @Test
    void sixthAttemptWithCorrectPasswordIsStillLockedAfterFiveFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").with(csrf())
                    .param("username", "lockoutuser")
                    .param("password", "wrong-password"))
               .andExpect(redirectedUrl("/login?error"));
        }

        // 6th attempt uses the CORRECT password -- must still be rejected because locked.
        mvc.perform(post("/login").with(csrf())
                .param("username", "lockoutuser")
                .param("password", "Tasty-Recipe123"))
           .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void anotherUsersCorrectLoginIsUnaffectedByADifferentAccountsLockout() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").with(csrf())
                    .param("username", "lockoutuser")
                    .param("password", "wrong-password"));
        }

        mvc.perform(post("/login").with(csrf())
                .param("username", "unaffecteduser")
                .param("password", "Tasty-Recipe123"))
           .andExpect(redirectedUrl("/explore"));
    }

    // Reproduces the bug: a successful login by EMAIL must clear the EMAIL-keyed failure
    // counter (the key LoginAttemptService actually records failures/lockout on), not just the
    // canonical-username-keyed one. Before the fix, TwoFactorAuthSuccessHandler reset only
    // authentication.getName() (the canonical username), so the email-keyed counter kept
    // accumulating across the "successful" login and a later legitimate attempt got wrongly
    // locked out within the 15-minute window.
    @Test
    void successfulLoginByEmailResetsTheEmailKeyedFailureCounter() throws Exception {
        String email = "emaillockout@example.com";

        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/login").with(csrf())
                    .param("username", email)
                    .param("password", "wrong-password"))
               .andExpect(redirectedUrl("/login?error"));
        }

        // Successful login by email -- must reset the email-keyed counter.
        mvc.perform(post("/login").with(csrf())
                .param("username", email)
                .param("password", "Tasty-Recipe123"))
           .andExpect(redirectedUrl("/explore"));

        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/login").with(csrf())
                    .param("username", email)
                    .param("password", "wrong-password"))
               .andExpect(redirectedUrl("/login?error"));
        }

        // Without the fix, 8 accumulated failures (4 + 4) would exceed MAX_ATTEMPTS (5) and
        // this correct-password attempt would be wrongly locked out.
        mvc.perform(post("/login").with(csrf())
                .param("username", email)
                .param("password", "Tasty-Recipe123"))
           .andExpect(redirectedUrl("/explore"));
    }
}

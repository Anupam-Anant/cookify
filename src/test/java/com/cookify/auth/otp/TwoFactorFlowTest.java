package com.cookify.auth.otp;

import com.cookify.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Several tests here issue more than one OTP for "mfauser" in quick succession (once via the
// login success handler, then again directly via otpService.issue in the test body) -- disable
// the resend cooldown so that reissue throttling never trips this intentional, rapid-reissue
// test pattern.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"cookify.otp.resend-cooldown-seconds=0"})
class TwoFactorFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired OtpService otpService;
    @Autowired JdbcTemplate jdbc;

    private User user;

    @BeforeEach
    void seed() {
        user = users.findByUsername("mfauser").orElseGet(() -> {
            User u = new User();
            u.setUsername("mfauser");
            u.setEmail("mfa@example.com");
            u.setPhone("+915555555555");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setTwoFactorEnabled(true);
            return users.save(u);
        });
    }

    @Test
    void loginWith2faRedirectsToChallenge() throws Exception {
        mvc.perform(post("/login").with(csrf())
                .param("username", "mfauser")
                .param("password", "Tasty-Recipe123"))
           .andExpect(redirectedUrl("/2fa"));
    }

    @Test
    void validOtpCompletesLogin() throws Exception {
        String code = otpService.issue(user.getId(), OtpPurpose.LOGIN_2FA);
        mvc.perform(post("/2fa").with(csrf())
                .sessionAttr("PRE_2FA_USER_ID", user.getId())
                .param("code", code))
           .andExpect(redirectedUrl("/explore"));
    }

    @Test
    void wrongOtpShowsError() throws Exception {
        otpService.issue(user.getId(), OtpPurpose.LOGIN_2FA);
        mvc.perform(post("/2fa").with(csrf())
                .sessionAttr("PRE_2FA_USER_ID", user.getId())
                .param("code", "000000"))
           .andExpect(status().isOk())
           .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid")));
    }

    @Test
    void rememberMeIsGatedBy2fa() throws Exception {
        mvc.perform(post("/login").with(csrf())
                .param("username", "mfauser")
                .param("password", "Tasty-Recipe123")
                .param("remember-me", "on"))
           .andExpect(redirectedUrl("/2fa"));

        int beforeOtp = jdbc.queryForObject(
            "SELECT COUNT(*) FROM persistent_logins WHERE username = ?", Integer.class, "mfauser");
        assertEquals(0, beforeOtp, "premature remember-me token must be cancelled before 2FA completes");

        String code = otpService.issue(user.getId(), OtpPurpose.LOGIN_2FA);
        mvc.perform(post("/2fa").with(csrf())
                .sessionAttr("PRE_2FA_USER_ID", user.getId())
                .sessionAttr("PRE_2FA_REMEMBER_ME", true)
                .param("code", code)
                .param("remember-me", "on"))
           .andExpect(redirectedUrl("/explore"));

        int afterOtp = jdbc.queryForObject(
            "SELECT COUNT(*) FROM persistent_logins WHERE username = ?", Integer.class, "mfauser");
        assertTrue(afterOtp >= 1, "remember-me token should be issued only after 2FA completes");
    }

    @Test
    void loginDoesNotPurgeOtherDeviceRememberMeTokens() throws Exception {
        jdbc.update("DELETE FROM persistent_logins WHERE series = ?", "series-other");
        jdbc.update("INSERT INTO persistent_logins(username, series, token, last_used) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
            "mfauser", "series-other", "tok-other");

        mvc.perform(post("/login").with(csrf())
                .param("username", "mfauser")
                .param("password", "Tasty-Recipe123")
                .param("remember-me", "on"))
           .andExpect(redirectedUrl("/2fa"));

        int stillPresent = jdbc.queryForObject(
            "SELECT COUNT(*) FROM persistent_logins WHERE series = ?", Integer.class, "series-other");
        assertEquals(1, stillPresent, "logging in again must not purge remember-me tokens for other devices");
    }

    @Test
    void preTwoFactorSessionIsNotAuthenticated() throws Exception {
        MvcResult result = mvc.perform(post("/login").with(csrf())
                .param("username", "mfauser")
                .param("password", "Tasty-Recipe123"))
           .andExpect(redirectedUrl("/2fa"))
           .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mvc.perform(get("/profile").session(session))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrlPattern("**/login"));
    }
}

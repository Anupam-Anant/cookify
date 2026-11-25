package com.cookify.auth.reset;

import com.cookify.auth.otp.OtpPurpose;
import com.cookify.auth.otp.OtpService;
import com.cookify.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired OtpService otpService;

    private User user;

    @BeforeEach
    void seed() {
        user = users.findByUsername("resetuser").orElseGet(() -> {
            User u = new User();
            u.setUsername("resetuser");
            u.setEmail("reset@example.com");
            u.setPhone("+913333333333");
            u.setPasswordHash(encoder.encode("Old-Password123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });
    }

    @Test
    void forgotAlwaysRedirects() throws Exception {
        mvc.perform(post("/password/forgot").with(csrf()).param("identifier", "nobody@nowhere.com"))
           .andExpect(redirectedUrl("/password/reset?sent"));
    }

    @Test
    void resetWithValidCodeChangesPassword() throws Exception {
        String code = otpService.issue(user.getId(), OtpPurpose.PASSWORD_RESET);
        mvc.perform(post("/password/reset").with(csrf())
                .param("identifier", "resetuser")
                .param("code", code)
                .param("newPassword", "Brand-New-Pass123"))
           .andExpect(redirectedUrl("/login?reset"));
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(encoder.matches("Brand-New-Pass123", reloaded.getPasswordHash())).isTrue();
    }
}

package com.cookify.auth.otp;

import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Seeds real User rows and uses their generated ids rather than hardcoded 1L/2L/3L:
// email_otp.user_id REFERENCES app_user(user_id) NOT NULL, so a hardcoded id with no
// matching app_user row would violate that FK constraint under H2.
@DataJpaTest
class OtpServiceTest {

    @Autowired EmailOtpRepository repo;
    @Autowired UserRepository users;
    OtpService service;

    @BeforeEach
    void setUp() { service = new OtpService(repo, new BCryptPasswordEncoder()); }

    private Long seedUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPhone("+91" + Math.abs(username.hashCode() % 1000000000));
        u.setPasswordHash("$2a$dummy");
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(u).getId();
    }

    @Test
    void issuedCodeVerifiesOnce() {
        Long userId = seedUser("otpuser1");
        String code = service.issue(userId, OtpPurpose.LOGIN_2FA);
        assertThat(code).hasSize(6).containsOnlyDigits();
        assertThat(service.verify(userId, OtpPurpose.LOGIN_2FA, code)).isTrue();
        // single-use: second verify fails
        assertThat(service.verify(userId, OtpPurpose.LOGIN_2FA, code)).isFalse();
    }

    @Test
    void wrongCodeFails() {
        Long userId = seedUser("otpuser2");
        service.issue(userId, OtpPurpose.PASSWORD_RESET);
        assertThat(service.verify(userId, OtpPurpose.PASSWORD_RESET, "000000")).isFalse();
    }

    @Test
    void expiredCodeFails() {
        Long userId = seedUser("otpuser3");
        String code = service.issue(userId, OtpPurpose.LOGIN_2FA);
        EmailOtp otp = repo.findFirstByUserIdAndPurposeAndConsumedFalseOrderByIdDesc(userId, OtpPurpose.LOGIN_2FA).orElseThrow();
        otp.setExpiresAt(otp.getExpiresAt().minusMinutes(20));
        repo.save(otp);
        assertThat(service.verify(userId, OtpPurpose.LOGIN_2FA, code)).isFalse();
    }

    @Test
    void reissuingOtpInvalidatesThePreviousCode() {
        Long userId = seedUser("otpuser4");
        // Cooldown 0 -- this test is exercising supersede, not throttling.
        OtpService zeroCooldown = new OtpService(repo, new BCryptPasswordEncoder(), 0);
        String firstCode = zeroCooldown.issue(userId, OtpPurpose.LOGIN_2FA);
        String secondCode = zeroCooldown.issue(userId, OtpPurpose.LOGIN_2FA);

        assertThat(zeroCooldown.verify(userId, OtpPurpose.LOGIN_2FA, firstCode))
            .as("superseded code must no longer verify")
            .isFalse();
        assertThat(zeroCooldown.verify(userId, OtpPurpose.LOGIN_2FA, secondCode))
            .as("newest code must still verify")
            .isTrue();
    }

    @Test
    void rapidReissueIsThrottledWhenCooldownIsNonZero() {
        Long userId = seedUser("otpuser5");
        OtpService throttled = new OtpService(repo, new BCryptPasswordEncoder(), 60);
        throttled.issue(userId, OtpPurpose.LOGIN_2FA);

        assertThatThrownBy(() -> throttled.issue(userId, OtpPurpose.LOGIN_2FA))
            .isInstanceOf(OtpThrottledException.class);
    }
}

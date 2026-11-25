package com.cookify.auth.otp;

import com.cookify.auth.CookifyUserDetails;
import com.cookify.auth.LoginAttemptService;
import com.cookify.email.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TwoFactorAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorAuthSuccessHandler.class);

    public static final String PRE_2FA_USER_ID = "PRE_2FA_USER_ID";
    public static final String PRE_2FA_REMEMBER_ME = "PRE_2FA_REMEMBER_ME";

    private final OtpService otpService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    public TwoFactorAuthSuccessHandler(OtpService otpService, EmailService emailService,
                                        LoginAttemptService loginAttemptService) {
        this.otpService = otpService;
        this.emailService = emailService;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CookifyUserDetails principal = (CookifyUserDetails) authentication.getPrincipal();
        var user = principal.getUser();
        // Successful password auth (whether or not 2FA follows) clears any accumulated
        // failed-login count. LoginAttemptFailureHandler/CookifyUserDetailsService record
        // failures and check lockout keyed on the SUBMITTED login identifier (the "username"
        // form field, which may be a username, email, or phone) -- NOT the canonical
        // authentication.getName(). Reset both keys so a user who logs in via email/phone
        // actually gets their failure counter cleared.
        String submittedIdentifier = request.getParameter("username");
        if (submittedIdentifier != null) {
            loginAttemptService.reset(submittedIdentifier);
        }
        loginAttemptService.reset(authentication.getName());
        if (user.isTwoFactorEnabled()) {
            try {
                String code = otpService.issue(user.getId(), OtpPurpose.LOGIN_2FA);
                emailService.sendOtp(user.getEmail(), code, "login");
            } catch (OtpThrottledException e) {
                // A code was already issued within the cooldown window (e.g. a rapid
                // double-submit of the login form) -- the previously emailed code is still
                // valid, so don't issue/send another one and don't fail the login.
                log.info("2FA OTP throttled userId={}", user.getId());
            }
            boolean remember = request.getParameter("remember-me") != null;
            request.getSession().setAttribute(PRE_2FA_REMEMBER_ME, remember);
            // CookifyRememberMeServices.loginSuccess() is a no-op for 2FA users, so no
            // premature token/cookie was ever created here — nothing to cancel or purge.
            SecurityContextHolder.clearContext();
            request.getSession().removeAttribute("SPRING_SECURITY_CONTEXT");
            request.getSession().setAttribute(PRE_2FA_USER_ID, user.getId());
            response.sendRedirect("/2fa");
        } else {
            response.sendRedirect("/explore");
        }
    }
}

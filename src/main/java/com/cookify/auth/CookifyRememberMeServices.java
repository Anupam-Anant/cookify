package com.cookify.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

/**
 * Remember-me services that defer token issuance for 2FA-enabled users: a persistent
 * login token must never be created before the OTP challenge is completed. For 2FA
 * users, {@link #loginSuccess} is a no-op (the premature token is never created in the
 * first place, so there's nothing to cancel/purge later and no other-device tokens are
 * ever touched). {@link #issueToken} is called explicitly by the 2FA controller once the
 * OTP has been verified, to issue the real token/cookie at that point.
 */
public class CookifyRememberMeServices extends PersistentTokenBasedRememberMeServices {

    public CookifyRememberMeServices(String key, UserDetailsService uds, PersistentTokenRepository tokenRepository) {
        super(key, uds, tokenRepository);
    }

    @Override
    public void loginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
        Object principal = successfulAuthentication.getPrincipal();
        if (principal instanceof CookifyUserDetails details && details.getUser().isTwoFactorEnabled()) {
            // Defer remember-me until 2FA completes — do NOT issue a token/cookie now.
            return;
        }
        super.loginSuccess(request, response, successfulAuthentication);
    }

    /**
     * Issues the persistent remember-me token/cookie unconditionally. Called explicitly
     * once the OTP challenge has been verified for a 2FA user.
     */
    public void issueToken(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
        onLoginSuccess(request, response, successfulAuthentication);
    }
}

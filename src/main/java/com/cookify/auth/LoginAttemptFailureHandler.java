package com.cookify.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Records a failed login attempt (keyed by the submitted {@code username} form field) against
 * {@link LoginAttemptService} before falling back to the default form-login failure behavior
 * ({@code /login?error}). Fires for every authentication failure, including the
 * {@link org.springframework.security.authentication.LockedException} thrown by
 * {@link CookifyUserDetailsService} once an identifier is already locked out.
 */
@Component
public class LoginAttemptFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public LoginAttemptFailureHandler(LoginAttemptService loginAttemptService) {
        super("/login?error");
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (username != null) {
            loginAttemptService.recordFailure(username);
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}

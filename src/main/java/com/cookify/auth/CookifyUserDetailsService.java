package com.cookify.auth;

import com.cookify.user.UserRepository;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CookifyUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final LoginAttemptService loginAttemptService;

    public CookifyUserDetailsService(UserRepository users, LoginAttemptService loginAttemptService) {
        this.users = users;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        if (loginAttemptService.isLocked(login)) {
            throw new LockedException("Account temporarily locked due to repeated failed logins. Try again later.");
        }
        return users.findByUsernameOrEmailOrPhone(login, login, login)
            .map(CookifyUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login));
    }
}

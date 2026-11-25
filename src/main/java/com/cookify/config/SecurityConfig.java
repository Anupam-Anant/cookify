package com.cookify.config;

import com.cookify.auth.CookifyRememberMeServices;
import com.cookify.auth.CookifyUserDetailsService;
import com.cookify.auth.LoginAttemptFailureHandler;
import com.cookify.auth.otp.TwoFactorAuthSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
public class SecurityConfig {

    // Dev gets a baked-in fallback (application.yml); prod (application-prod.yml) has NO
    // default here -- it MUST supply COOKIFY_REMEMBER_ME_KEY or startup fails fast.
    @Value("${cookify.remember-me.key}")
    private String rememberMeKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    @Bean
    public CookifyRememberMeServices rememberMeServices(CookifyUserDetailsService userDetailsService,
                                                         PersistentTokenRepository tokenRepository) {
        CookifyRememberMeServices services = new CookifyRememberMeServices(
            rememberMeKey, userDetailsService, tokenRepository);
        services.setParameter("remember-me");
        services.setTokenValiditySeconds(1209600); // 14 days
        return services;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            TwoFactorAuthSuccessHandler twoFactorAuthSuccessHandler,
                                            LoginAttemptFailureHandler loginAttemptFailureHandler,
                                            CookifyRememberMeServices rememberMeServices) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/signup", "/password/**", "/2fa",
                                 "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                // SockJS/STOMP handshake (info + websocket upgrade, both GET — not subject to
                // CSRF): permit here so the chat UI can open a session; the actual send/receive
                // of messages is still gated per-user inside ChatStompController via Principal.
                .requestMatchers("/ws/**").permitAll()
                // Owner-only recipe routes MUST be declared before the broader
                // GET /recipes/* permitAll below, otherwise that broad rule would
                // wrongly open /recipes/new and /recipes/*/edit to the public.
                .requestMatchers("/recipes/new", "/recipes/*/edit").authenticated()
                .requestMatchers(HttpMethod.GET, "/explore", "/recipes/*").permitAll()
                // Public creator profile (GET only — a single path segment, so this never
                // matches the two-segment POST /creators/{id}/subscribe|unsubscribe routes,
                // which stay behind anyRequest().authenticated() below).
                .requestMatchers(HttpMethod.GET, "/creators/*").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(twoFactorAuthSuccessHandler)
                .failureHandler(loginAttemptFailureHandler)
                .permitAll())
            .rememberMe(rm -> rm
                .rememberMeServices(rememberMeServices))
            .logout(logout -> logout.logoutSuccessUrl("/"))
            .headers(headers -> headers
                // Widened to allow the Google Translate widget: its script
                // (translate.google.com / translate.googleapis.com / www.gstatic.com), its
                // fonts (fonts.googleapis.com / fonts.gstatic.com), and its translation
                // overlay iframe (frame-src translate.google.com). 'self' and the prior
                // sources are kept intact.
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; "
                    + "img-src 'self' data: https://translate.google.com https://translate.googleapis.com https://www.gstatic.com; "
                    + "style-src 'self' 'unsafe-inline' https://www.gstatic.com https://fonts.googleapis.com; "
                    + "script-src 'self' https://translate.google.com https://translate.googleapis.com https://www.gstatic.com; "
                    + "font-src 'self' https://fonts.gstatic.com; "
                    + "frame-src 'self' https://translate.google.com")));
        return http.build();
    }
}

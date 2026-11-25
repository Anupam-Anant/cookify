package com.cookify;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Renders the login form directly. Spring Security's formLogin()
     * references loginPage("/login"), but that alone does not register a
     * GET handler — without this mapping, an unauthenticated GET /login
     * falls through to Spring Security's default generated login page (or,
     * on a container with URL-rewriting enabled, a jsessionid redirect)
     * instead of the real auth/login Thymeleaf view.
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}

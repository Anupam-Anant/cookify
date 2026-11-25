package com.cookify.auth;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/signup")
public class SignupController {

    private final RegistrationService registration;

    public SignupController(RegistrationService registration) {
        this.registration = registration;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "auth/signup";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") RegistrationForm form,
                         BindingResult binding) {
        if (binding.hasErrors()) {
            return "auth/signup";
        }
        try {
            registration.register(form);
        } catch (RegistrationException ex) {
            binding.rejectValue(ex.getField(), "registration", ex.getMessage());
            return "auth/signup";
        }
        return "redirect:/login?registered";
    }
}

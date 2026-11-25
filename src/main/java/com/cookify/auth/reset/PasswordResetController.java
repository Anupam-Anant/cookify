package com.cookify.auth.reset;

import com.cookify.auth.PasswordPolicy;
import com.cookify.auth.otp.OtpPurpose;
import com.cookify.auth.otp.OtpService;
import com.cookify.auth.otp.OtpThrottledException;
import com.cookify.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/password")
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final UserRepository users;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordPolicy policy;
    private final PasswordEncoder encoder;

    public PasswordResetController(UserRepository users, OtpService otpService, EmailService emailService,
                                   PasswordPolicy policy, PasswordEncoder encoder) {
        this.users = users;
        this.otpService = otpService;
        this.emailService = emailService;
        this.policy = policy;
        this.encoder = encoder;
    }

    @GetMapping("/forgot")
    public String forgotForm() { return "auth/forgot"; }

    @PostMapping("/forgot")
    public String forgot(@RequestParam String identifier) {
        lookup(identifier).ifPresent(u -> {
            try {
                String code = otpService.issue(u.getId(), OtpPurpose.PASSWORD_RESET);
                emailService.sendPasswordResetCode(u.getEmail(), code);
            } catch (OtpThrottledException e) {
                // A code was already issued within the cooldown window -- the previously
                // emailed code is still valid, so no new one is sent. The redirect below is
                // identical either way, so this never leaks whether the identifier exists.
                log.info("Password reset OTP throttled userId={}", u.getId());
            }
        });
        return "redirect:/password/reset?sent";
    }

    @GetMapping("/reset")
    public String resetForm() { return "auth/reset"; }

    @PostMapping("/reset")
    public String reset(@RequestParam String identifier, @RequestParam String code,
                        @RequestParam String newPassword, Model model) {
        Optional<User> found = lookup(identifier);
        if (found.isEmpty() || !otpService.verify(found.get().getId(), OtpPurpose.PASSWORD_RESET, code)) {
            model.addAttribute("error", "Invalid identifier or code.");
            return "auth/reset";
        }
        List<String> pwErrors = policy.validate(newPassword);
        if (!pwErrors.isEmpty()) {
            model.addAttribute("error", String.join(" ", pwErrors));
            return "auth/reset";
        }
        User u = found.get();
        u.setPasswordHash(encoder.encode(newPassword));
        users.save(u);
        return "redirect:/login?reset";
    }

    private Optional<User> lookup(String identifier) {
        return users.findByUsernameOrEmailOrPhone(identifier, identifier, identifier);
    }
}

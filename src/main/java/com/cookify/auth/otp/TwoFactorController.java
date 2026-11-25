package com.cookify.auth.otp;

import com.cookify.auth.CookifyRememberMeServices;
import com.cookify.auth.CookifyUserDetails;
import com.cookify.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import static com.cookify.auth.otp.TwoFactorAuthSuccessHandler.PRE_2FA_REMEMBER_ME;
import static com.cookify.auth.otp.TwoFactorAuthSuccessHandler.PRE_2FA_USER_ID;

@Controller
@RequestMapping("/2fa")
public class TwoFactorController {

    private final OtpService otpService;
    private final UserRepository users;
    private final CookifyRememberMeServices rememberMeServices;
    private final HttpSessionSecurityContextRepository contextRepo = new HttpSessionSecurityContextRepository();

    public TwoFactorController(OtpService otpService, UserRepository users, CookifyRememberMeServices rememberMeServices) {
        this.otpService = otpService;
        this.users = users;
        this.rememberMeServices = rememberMeServices;
    }

    @GetMapping
    public String challenge(HttpSession session, Model model) {
        if (session.getAttribute(PRE_2FA_USER_ID) == null) return "redirect:/login";
        Boolean remember = (Boolean) session.getAttribute(PRE_2FA_REMEMBER_ME);
        model.addAttribute("rememberMe", Boolean.TRUE.equals(remember));
        return "auth/twofa";
    }

    @PostMapping
    public String verify(@RequestParam String code, HttpSession session,
                         HttpServletRequest request, HttpServletResponse response, Model model) {
        Long userId = (Long) session.getAttribute(PRE_2FA_USER_ID);
        if (userId == null) return "redirect:/login";

        if (!otpService.verify(userId, OtpPurpose.LOGIN_2FA, code)) {
            model.addAttribute("error", "Invalid or expired code.");
            return "auth/twofa";
        }
        var user = users.findById(userId).orElseThrow();
        var principal = new CookifyUserDetails(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        contextRepo.saveContext(ctx, request, response);
        if (Boolean.TRUE.equals(session.getAttribute(PRE_2FA_REMEMBER_ME))) {
            rememberMeServices.issueToken(request, response, auth);
        }
        session.removeAttribute(PRE_2FA_REMEMBER_ME);
        session.removeAttribute(PRE_2FA_USER_ID);
        return "redirect:/explore";
    }
}

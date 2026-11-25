package com.cookify.chat;

import com.cookify.auth.CookifyUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renders the chat UI ({@code templates/chat.html}): pick another user, view history
 * (loaded client-side from {@link ChatRestController}'s GET endpoint), and send live via
 * the STOMP endpoint (see {@link com.cookify.config.WebSocketConfig}, {@link ChatStompController}).
 * Authenticated only, via the default {@code anyRequest().authenticated()} rule.
 */
@Controller
public class ChatPageController {

    @GetMapping("/chat")
    public String chat(@AuthenticationPrincipal CookifyUserDetails principal, Model model) {
        model.addAttribute("currentUserId", principal.getUser().getId());
        model.addAttribute("currentUsername", principal.getUser().getUsername());
        return "chat";
    }
}

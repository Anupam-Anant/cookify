package com.cookify.chat;

import com.cookify.auth.CookifyUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * JSON history + send endpoints for 1:1 chat. Authenticated only (see SecurityConfig's
 * {@code anyRequest().authenticated()} default); the live send path for the chat UI is the
 * STOMP endpoint ({@link ChatStompController}) — this POST exists for a plain non-WebSocket
 * client / test harness.
 */
@RestController
@RequestMapping("/chat/{otherUserId}/messages")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<ChatMessage> history(@AuthenticationPrincipal CookifyUserDetails principal,
                                      @PathVariable Long otherUserId) {
        return chatService.conversation(principal.getUser().getId(), otherUserId);
    }

    @PostMapping
    public ChatMessage send(@AuthenticationPrincipal CookifyUserDetails principal,
                             @PathVariable Long otherUserId,
                             @RequestParam String body) {
        return chatService.send(principal.getUser().getId(), otherUserId, body);
    }
}

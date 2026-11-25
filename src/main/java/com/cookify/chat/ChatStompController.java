package com.cookify.chat;

import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Live chat delivery over STOMP: a client sends to {@code /app/chat.send} with
 * {@link ChatSendRequest}; this persists the message via {@link ChatService} (the same
 * validation/persistence path used by {@link ChatRestController}) and republishes the saved
 * message to {@code /topic/chat.{conversationKey}} so both participants' subscribed
 * clients receive it live. Thin delegator — no business logic of its own.
 */
@Controller
public class ChatStompController {

    private final ChatService chatService;
    private final UserRepository users;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatStompController(ChatService chatService, UserRepository users,
                                SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.users = users;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(ChatSendRequest request, Principal principal) {
        Long senderId = users.findByUsername(principal.getName())
            .map(User::getId)
            .orElseThrow();
        ChatMessage saved = chatService.send(senderId, request.recipientId(), request.body());
        messagingTemplate.convertAndSend("/topic/chat." + conversationKey(senderId, request.recipientId()), saved);
    }

    /** Deterministic topic key for a pair of user ids, order-independent. */
    static String conversationKey(Long a, Long b) {
        return a <= b ? a + "-" + b : b + "-" + a;
    }
}

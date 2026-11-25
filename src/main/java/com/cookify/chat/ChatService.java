package com.cookify.chat;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 1:1 direct-message business logic over {@link ChatMessage}: persisting a sent message and
 * reading the time-ordered conversation between two users (both directions).
 */
@Service
public class ChatService {

    private final ChatMessageRepository chatMessages;

    public ChatService(ChatMessageRepository chatMessages) {
        this.chatMessages = chatMessages;
    }

    public ChatMessage send(Long senderId, Long recipientId, String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message body must not be blank");
        }
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setRecipientId(recipientId);
        message.setBody(body);
        return chatMessages.save(message);
    }

    public List<ChatMessage> conversation(Long userA, Long userB) {
        return chatMessages.findConversation(userA, userB);
    }
}

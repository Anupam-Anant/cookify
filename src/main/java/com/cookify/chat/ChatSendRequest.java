package com.cookify.chat;

/** STOMP payload for {@code @MessageMapping("/chat.send")}: who to send to and what to say. */
public record ChatSendRequest(Long recipientId, String body) {
}

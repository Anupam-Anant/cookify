package com.cookify.chat;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A single 1:1 direct message between two users. Table {@code chat_message} (V1 migration):
 * {@code sender_id}, {@code recipient_id}, {@code body}, {@code created_at}.
 */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long v) { this.senderId = v; }
    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long v) { this.recipientId = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

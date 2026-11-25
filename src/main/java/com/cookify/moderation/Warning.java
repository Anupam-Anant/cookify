package com.cookify.moderation;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "warning")
public class Warning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

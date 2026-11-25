package com.cookify.subscription;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Follow relationship: a subscriber follows a creator. Table {@code subscription}
 * (V1 migration) has a UNIQUE(subscriber_id, creator_id) constraint. ModerationService's
 * banAndPurge deletes a banned user's subscription rows on both sides of the relationship;
 * subscribe/unsubscribe/notify-on-upload business logic lives in SubscriptionService.
 */
@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscriber_id", nullable = false)
    private Long subscriberId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getSubscriberId() { return subscriberId; }
    public void setSubscriberId(Long v) { this.subscriberId = v; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long v) { this.creatorId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

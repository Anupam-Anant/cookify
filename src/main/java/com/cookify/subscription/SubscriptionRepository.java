package com.cookify.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findBySubscriberIdAndCreatorId(Long subscriberId, Long creatorId);
    List<Subscription> findByCreatorId(Long creatorId);
    boolean existsBySubscriberIdAndCreatorId(Long subscriberId, Long creatorId);
    long countByCreatorId(Long creatorId);
    long countBySubscriberId(Long subscriberId);

    // Needed by ModerationService.banAndPurge to purge a banned user's subscription rows on
    // both sides of the relationship.
    void deleteBySubscriberId(Long subscriberId);
    void deleteByCreatorId(Long creatorId);
}

package com.cookify.subscription;

import com.cookify.email.EmailService;
import com.cookify.recipe.Recipe;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Follow/subscribe business logic on top of the {@link Subscription} entity: subscribing emails
 * the creator, and {@link #notifyNewRecipe} emails every subscriber whenever that creator
 * uploads a new recipe (wired from {@code RecipeService.create}).
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptions;
    private final UserRepository users;
    private final EmailService emailService;

    public SubscriptionService(SubscriptionRepository subscriptions, UserRepository users, EmailService emailService) {
        this.subscriptions = subscriptions;
        this.users = users;
        this.emailService = emailService;
    }

    @Transactional
    public void subscribe(Long subscriberId, Long creatorId) {
        if (subscriptions.existsBySubscriberIdAndCreatorId(subscriberId, creatorId)) {
            return;
        }
        Subscription s = new Subscription();
        s.setSubscriberId(subscriberId);
        s.setCreatorId(creatorId);
        subscriptions.save(s);
        log.info("User subscribed subscriberId={} creatorId={}", subscriberId, creatorId);

        users.findById(creatorId).ifPresent(creator -> emailService.sendNotification(
            creator.getEmail(),
            "COOKify: You have a new subscriber",
            "Good news - you have a new subscriber on COOKify!"));
    }

    @Transactional
    public void unsubscribe(Long subscriberId, Long creatorId) {
        subscriptions.findBySubscriberIdAndCreatorId(subscriberId, creatorId)
            .ifPresent(subscriptions::delete);
    }

    public void notifyNewRecipe(Long creatorId, Recipe recipe) {
        List<Subscription> subscribers = subscriptions.findByCreatorId(creatorId);
        if (subscribers.isEmpty()) {
            return;
        }
        String creatorName = users.findById(creatorId).map(User::getUsername).orElse("A creator you follow");
        String body = creatorName + " just posted a new recipe: " + recipe.getRecipeName()
            + "\nCheck it out: /recipes/" + recipe.getId();
        for (Subscription s : subscribers) {
            users.findById(s.getSubscriberId()).ifPresent(subscriber -> emailService.sendNotification(
                subscriber.getEmail(),
                "COOKify: New recipe from " + creatorName,
                body));
        }
        log.info("New-upload notifications sent creatorId={} recipeId={} subscriberCount={}",
            creatorId, recipe.getId(), subscribers.size());
    }

    public boolean isSubscribed(Long subscriberId, Long creatorId) {
        return subscriptions.existsBySubscriberIdAndCreatorId(subscriberId, creatorId);
    }
}

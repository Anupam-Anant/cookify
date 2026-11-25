package com.cookify.comment;

import com.cookify.email.EmailService;
import com.cookify.moderation.ContentFilter;
import com.cookify.moderation.ModerationService;
import com.cookify.rating.RecipeRating;
import com.cookify.rating.RecipeRatingRepository;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Comments are stored on the user's {@code recipe_rating} row ({@code comments} column) —
 * the same one-hot-star row used by {@link com.cookify.rating.RatingService}. Every comment
 * is NSFW-gated by {@link ContentFilter} before storage; a violation is never persisted and
 * instead raises a moderation warning + email, per {@link ModerationService#warn}.
 */
@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final RecipeRatingRepository ratings;
    private final ContentFilter contentFilter;
    private final ModerationService moderationService;
    private final EmailService emailService;
    private final UserRepository users;

    public CommentService(RecipeRatingRepository ratings,
                           ContentFilter contentFilter,
                           ModerationService moderationService,
                           EmailService emailService,
                           UserRepository users) {
        this.ratings = ratings;
        this.contentFilter = contentFilter;
        this.moderationService = moderationService;
        this.emailService = emailService;
        this.users = users;
    }

    @Transactional
    public void addComment(Long recipeId, Long userId, String text) {
        if (!contentFilter.isClean(text)) {
            log.warn("NSFW comment rejected userId={} recipeId={}", userId, recipeId);
            moderationService.warn(userId, "NSFW comment blocked");
            users.findById(userId).ifPresent(u -> emailService.sendNotification(
                u.getEmail(),
                "COOKify content warning",
                "Your comment was blocked for inappropriate content. "
                    + "Repeated violations may result in your account being suspended."));
            throw new CommentRejectedException("Comment blocked: inappropriate content");
        }

        RecipeRating rating = ratings.findByRecipeIdAndUserId(recipeId, userId)
            .orElseGet(() -> {
                RecipeRating r = new RecipeRating();
                r.setRecipeId(recipeId);
                r.setUserId(userId);
                return r;
            });
        rating.setComments(text);
        ratings.save(rating);
    }

    @Transactional(readOnly = true)
    public List<CommentView> comments(Long recipeId) {
        return ratings.findByRecipeId(recipeId).stream()
            .filter(r -> r.getComments() != null && !r.getComments().isBlank())
            .map(r -> new CommentView(usernameOf(r.getUserId()), r.getComments()))
            .toList();
    }

    private String usernameOf(Long userId) {
        return users.findById(userId).map(User::getUsername).orElse("unknown");
    }
}

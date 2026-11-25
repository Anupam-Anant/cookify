package com.cookify.moderation;

import com.cookify.category.RecipeCategoryRepository;
import com.cookify.chat.ChatMessageRepository;
import com.cookify.rating.RecipeRatingRepository;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeRepository;
import com.cookify.saved.SavedRecipeRepository;
import com.cookify.subscription.SubscriptionRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 3-strike moderation: {@link #warn} records a warning and bumps {@code user.warningCount};
 * the 3rd warning triggers {@link #banAndPurge}, which suspends the account and deletes all
 * of the user's content.
 *
 * {@code banAndPurge} injects {@link RecipeCategoryRepository} because a purged recipe's
 * {@code recipe_category} row (FK to {@code recipe}, no cascade) must be deleted before the
 * recipe itself, exactly as {@code RecipeService.delete} already does.
 *
 * It also has to account for {@code saved_recipe} (FK {@code recipe_id} to {@code recipe}, no
 * cascade, V4 migration): if anyone had bookmarked one of the banned user's recipes,
 * {@code recipes.deleteAll(own)} below would throw a FK-constraint violation — and because
 * {@link #warn} is {@code REQUIRES_NEW} and calls this method inline, that exception would
 * silently roll back the warning, the count increment, and the ban together. So this also
 * injects {@link SavedRecipeRepository} (to clear bookmarks of the purged recipes BEFORE
 * deleting them, and the banned user's own bookmarks) and {@link ChatMessageRepository} (to
 * purge the banned user's DMs).
 */
@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);
    private static final int BAN_THRESHOLD = 3;

    private final WarningRepository warnings;
    private final UserRepository users;
    private final RecipeRepository recipes;
    private final RecipeCategoryRepository categories;
    private final RecipeRatingRepository ratings;
    private final SubscriptionRepository subscriptions;
    private final SavedRecipeRepository savedRecipes;
    private final ChatMessageRepository chatMessages;

    public ModerationService(WarningRepository warnings,
                              UserRepository users,
                              RecipeRepository recipes,
                              RecipeCategoryRepository categories,
                              RecipeRatingRepository ratings,
                              SubscriptionRepository subscriptions,
                              SavedRecipeRepository savedRecipes,
                              ChatMessageRepository chatMessages) {
        this.warnings = warnings;
        this.users = users;
        this.recipes = recipes;
        this.categories = categories;
        this.ratings = ratings;
        this.subscriptions = subscriptions;
        this.savedRecipes = savedRecipes;
        this.chatMessages = chatMessages;
    }

    // REQUIRES_NEW: callers such as CommentService.addComment call warn(...) and then throw
    // (to reject the NSFW content) within their own @Transactional method. With the default
    // REQUIRED propagation, warn's writes would join that caller transaction and be rolled
    // back by the very exception warn's caller throws afterwards — silently erasing the
    // warning, the warningCount increment, and any 3rd-strike banAndPurge. REQUIRES_NEW
    // commits this warning (and a resulting ban) in its own transaction, independent of
    // whatever the caller does next.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void warn(Long userId, String reason) {
        Warning w = new Warning();
        w.setUserId(userId);
        w.setReason(reason);
        warnings.save(w);

        User user = requireUser(userId);
        user.setWarningCount(user.getWarningCount() + 1);
        users.save(user);
        log.info("Warning issued userId={} warningCount={}", userId, user.getWarningCount());

        if (user.getWarningCount() >= BAN_THRESHOLD) {
            banAndPurge(userId);
        }
    }

    @Transactional
    public void banAndPurge(Long userId) {
        log.warn("Banning and purging user userId={}", userId);
        User user = requireUser(userId);
        user.setAccountStatus(AccountStatus.SUSPENDED);
        users.save(user);

        List<Recipe> own = recipes.findByRecipeCreatorId(userId);
        List<Long> ownRecipeIds = own.stream().map(Recipe::getId).toList();
        for (Recipe recipe : own) {
            ratings.deleteByRecipeId(recipe.getId());
            categories.deleteById(recipe.getId());
        }
        // Must run BEFORE recipes.deleteAll(own): anyone's bookmark of one of these recipes
        // (saved_recipe.recipe_id FK, no cascade) would otherwise block the recipe delete.
        if (!ownRecipeIds.isEmpty()) {
            savedRecipes.deleteByRecipeIdIn(ownRecipeIds);
        }
        recipes.deleteAll(own);

        ratings.deleteByUserId(userId);
        subscriptions.deleteBySubscriberId(userId);
        subscriptions.deleteByCreatorId(userId);

        // Banned user's own bookmarks and DMs (completeness, not FK-required).
        savedRecipes.deleteByUserId(userId);
        chatMessages.deleteBySenderIdOrRecipientId(userId, userId);
    }

    public boolean isBanned(Long userId) {
        return users.findById(userId)
            .map(u -> u.getAccountStatus() == AccountStatus.SUSPENDED)
            .orElse(false);
    }

    private User requireUser(Long userId) {
        return users.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("No such user: " + userId));
    }
}

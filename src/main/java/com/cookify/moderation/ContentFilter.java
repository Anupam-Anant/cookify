package com.cookify.moderation;

/**
 * NSFW / profanity gate consulted before any user-generated text (comments, etc.)
 * is persisted. Pluggable so the rule-based default ({@link RuleBasedContentFilter})
 * can later be swapped for an ML- or external-API-backed implementation without
 * touching callers.
 */
public interface ContentFilter {
    boolean isClean(String text);
}

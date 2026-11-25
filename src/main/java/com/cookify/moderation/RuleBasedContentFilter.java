package com.cookify.moderation;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Default {@link ContentFilter}: a small, documented, case-insensitive banned-word
 * list matched on word boundaries so innocent words that merely contain a banned
 * substring (e.g. "class" vs. banned "ass") are never flagged.
 *
 * Deliberately naive. Replace this bean with an ML/external-moderation-API-backed
 * {@link ContentFilter} implementation for production-grade coverage without
 * changing any caller (CommentService, etc.).
 */
@Service
public class RuleBasedContentFilter implements ContentFilter {

    private static final Set<String> BANNED_WORDS = Set.of(
        "badword1", "ass", "bastard", "bitch", "fuck", "shit"
    );

    private static final Pattern BANNED_PATTERN = Pattern.compile(
        "\\b(" + String.join("|", BANNED_WORDS) + ")\\b",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean isClean(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        return !BANNED_PATTERN.matcher(text).find();
    }
}

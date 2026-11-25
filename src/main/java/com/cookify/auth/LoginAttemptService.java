package com.cookify.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed login attempts per login identifier (username/email/phone, lowercased) and
 * locks an identifier out for a fixed window once it accumulates too many failures. In-memory
 * only -- acceptable for a single-instance deployment; a multi-instance deployment would need
 * a shared store (e.g. Redis) instead.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final long LOCK_WINDOW_MINUTES = 15;

    private final ConcurrentHashMap<String, Attempt> attemptsByKey = new ConcurrentHashMap<>();

    public void recordFailure(String key) {
        String normalized = normalize(key);
        if (normalized.isEmpty()) {
            return;
        }
        attemptsByKey.compute(normalized, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null || isWindowExpired(existing, now)) {
                return new Attempt(1, now);
            }
            return new Attempt(existing.count + 1, existing.firstFailureAt);
        });
    }

    public void reset(String key) {
        attemptsByKey.remove(normalize(key));
    }

    public boolean isLocked(String key) {
        String normalized = normalize(key);
        Attempt attempt = attemptsByKey.get(normalized);
        if (attempt == null) {
            return false;
        }
        if (isWindowExpired(attempt, Instant.now())) {
            attemptsByKey.remove(normalized, attempt);
            return false;
        }
        return attempt.count >= MAX_ATTEMPTS;
    }

    private boolean isWindowExpired(Attempt attempt, Instant now) {
        return attempt.firstFailureAt.plus(LOCK_WINDOW_MINUTES, ChronoUnit.MINUTES).isBefore(now);
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private record Attempt(int count, Instant firstFailureAt) {
    }
}

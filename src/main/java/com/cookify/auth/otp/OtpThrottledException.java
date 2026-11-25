package com.cookify.auth.otp;

/**
 * Thrown by {@link OtpService#issue} when a new code is requested for an (userId, purpose)
 * pair before {@code cookify.otp.resend-cooldown-seconds} has elapsed since the last one was
 * issued. Callers should handle this gracefully -- the previously issued code is still valid
 * and was already delivered, so there is nothing to retry or surface as a hard error.
 */
public class OtpThrottledException extends RuntimeException {
    public OtpThrottledException(String message) {
        super(message);
    }
}

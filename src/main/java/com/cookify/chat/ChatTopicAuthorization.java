package com.cookify.chat;

/**
 * Pure authorization check for STOMP chat subscriptions: only the two participants of a
 * conversation may subscribe to its topic. Destinations look like
 * {@code /topic/chat.{idA}-{idB}} (see {@link ChatStompController#conversationKey}), where the
 * two ids are plain numeric user ids in either order.
 *
 * <p>Kept framework-free and side-effect-free so it is trivially unit-testable; the actual
 * STOMP-channel enforcement (resolving the principal, rejecting the SUBSCRIBE frame) lives in
 * {@code WebSocketConfig}'s inbound-channel interceptor, which is a thin caller of this method.
 */
public final class ChatTopicAuthorization {

    private static final String PREFIX = "/topic/chat.";

    private ChatTopicAuthorization() {
    }

    public static boolean isParticipant(String destination, Long userId) {
        if (destination == null || userId == null || !destination.startsWith(PREFIX)) {
            return false;
        }
        String key = destination.substring(PREFIX.length());
        int dash = key.indexOf('-');
        if (dash <= 0 || dash == key.length() - 1) {
            return false;
        }
        try {
            long a = Long.parseLong(key.substring(0, dash));
            long b = Long.parseLong(key.substring(dash + 1));
            return userId == a || userId == b;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

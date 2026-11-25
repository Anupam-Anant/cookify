package com.cookify.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the chat STOMP-subscription authorization rule: only the two participants
 * of a conversation may subscribe to its /topic/chat.{a}-{b} destination. Deliberately NOT a live
 * STOMP-client test (stall-prone) — this is the fast, deterministic guard for the fix.
 */
class ChatTopicAuthorizationTest {

    @Test
    void bothParticipantsAreAuthorized() {
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.1-2", 1L)).isTrue();
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.1-2", 2L)).isTrue();
    }

    @Test
    void thirdPartyIsRejected() {
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.1-2", 3L)).isFalse();
    }

    @Test
    void orderOfIdsInTheKeyDoesNotMatter() {
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.2-7", 7L)).isTrue();
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.2-7", 2L)).isTrue();
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.2-7", 5L)).isFalse();
    }

    @Test
    void malformedOrMissingInputIsRejected() {
        assertThat(ChatTopicAuthorization.isParticipant(null, 1L)).isFalse();
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.1-2", null)).isFalse();
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.", 1L)).isFalse();
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.abc-def", 1L)).isFalse();
        assertThat(ChatTopicAuthorization.isParticipant("/topic/chat.1", 1L)).isFalse();
        assertThat(ChatTopicAuthorization.isParticipant("/topic/other.1-2", 1L)).isFalse();
    }
}

package com.cookify.chat;

import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @SpringBootTest against the real (shared, non-rolled-back) H2 "cookify" instance —
// same pattern as SubscriptionServiceTest/RecipeSocialControllerTest. Usernames are unique
// to this test class/method so re-running is safe.
@SpringBootTest
class ChatServiceTest {

    @Autowired ChatService chatService;
    @Autowired ChatMessageRepository chatMessages;
    @Autowired UserRepository users;

    private Long seedUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPhone("+91" + Math.abs(username.hashCode() % 1000000000));
        u.setPasswordHash("$2a$dummy");
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(u).getId();
    }

    @Test
    void sendPersistsMessage() {
        Long a = seedUser("chat1a");
        Long b = seedUser("chat1b");

        ChatMessage saved = chatService.send(a, b, "Hey there!");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSenderId()).isEqualTo(a);
        assertThat(saved.getRecipientId()).isEqualTo(b);
        assertThat(saved.getBody()).isEqualTo("Hey there!");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void sendRejectsBlankBody() {
        Long a = seedUser("chat2a");
        Long b = seedUser("chat2b");

        assertThatThrownBy(() -> chatService.send(a, b, "   "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chatService.send(a, b, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conversationReturnsBothDirectionsTimeOrdered() throws InterruptedException {
        Long a = seedUser("chat3a");
        Long b = seedUser("chat3b");

        chatService.send(a, b, "first from a");
        Thread.sleep(5);
        chatService.send(b, a, "second from b");
        Thread.sleep(5);
        chatService.send(a, b, "third from a");

        List<ChatMessage> convo = chatService.conversation(a, b);

        assertThat(convo).hasSize(3);
        assertThat(convo.get(0).getBody()).isEqualTo("first from a");
        assertThat(convo.get(1).getBody()).isEqualTo("second from b");
        assertThat(convo.get(2).getBody()).isEqualTo("third from a");

        // Order of arguments should not matter.
        List<ChatMessage> convoReversed = chatService.conversation(b, a);
        assertThat(convoReversed).hasSize(3);
        assertThat(convoReversed.get(0).getBody()).isEqualTo("first from a");
    }

    @Test
    void conversationDoesNotLeakOtherUsersMessages() {
        Long a = seedUser("chat4a");
        Long b = seedUser("chat4b");
        Long c = seedUser("chat4c");

        chatService.send(a, b, "a to b");
        chatService.send(a, c, "a to c");

        List<ChatMessage> convo = chatService.conversation(a, b);

        assertThat(convo).hasSize(1);
        assertThat(convo.get(0).getBody()).isEqualTo("a to b");
    }
}

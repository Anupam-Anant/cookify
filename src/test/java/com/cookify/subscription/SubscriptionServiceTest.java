package com.cookify.subscription;

import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// @SpringBootTest + GreenMail (same pattern as EmailServiceTest): SubscriptionService sends
// real emails via EmailService/JavaMailSender, which @DataJpaTest does not load. This shares
// the app's configured H2 "cookify" instance with other @SpringBootTest classes (no
// transactional rollback), so every seeded username below is unique to this test class/method.
@SpringBootTest
@TestPropertySource(properties = {"spring.mail.host=localhost", "spring.mail.port=3025"})
class SubscriptionServiceTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired SubscriptionService service;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;

    @BeforeEach
    void purgeInbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

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
    void subscribingEmailsTheCreator() {
        Long creatorId = seedUser("sub5creator1");
        Long subscriberId = seedUser("sub5fan1");

        service.subscribe(subscriberId, creatorId);

        assertThat(service.isSubscribed(subscriberId, creatorId)).isTrue();
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] msgs = greenMail.getReceivedMessages();
            assertThat(msgs).hasSize(1);
            assertThat(msgs[0].getAllRecipients()[0].toString()).isEqualTo("sub5creator1@example.com");
            assertThat(msgs[0].getSubject()).containsIgnoringCase("subscriber");
        });
    }

    @Test
    void duplicateSubscribeIsNoOpAndSendsNoSecondEmail() {
        Long creatorId = seedUser("sub5creator2");
        Long subscriberId = seedUser("sub5fan2");

        service.subscribe(subscriberId, creatorId);
        await().atMost(ofSeconds(5)).untilAsserted(() ->
            assertThat(greenMail.getReceivedMessages()).hasSize(1));

        service.subscribe(subscriberId, creatorId);

        await().during(ofSeconds(1)).atMost(ofSeconds(3)).untilAsserted(() ->
            assertThat(greenMail.getReceivedMessages()).hasSize(1));
        assertThat(subscriptions.countByCreatorId(creatorId)).isEqualTo(1);
    }

    @Test
    void unsubscribeRemovesSubscription() {
        Long creatorId = seedUser("sub5creator3");
        Long subscriberId = seedUser("sub5fan3");

        service.subscribe(subscriberId, creatorId);
        service.unsubscribe(subscriberId, creatorId);

        assertThat(service.isSubscribed(subscriberId, creatorId)).isFalse();
    }

    @Test
    void notifyNewRecipeEmailsAllSubscribersWithTitleAndLink() throws Exception {
        Long creatorId = seedUser("sub5creator4");
        Long subscriberA = seedUser("sub5fan4a");
        Long subscriberB = seedUser("sub5fan4b");
        service.subscribe(subscriberA, creatorId);
        service.subscribe(subscriberB, creatorId);
        await().atMost(ofSeconds(5)).untilAsserted(() ->
            assertThat(greenMail.getReceivedMessages()).hasSize(2));
        greenMail.purgeEmailFromAllMailboxes();

        Recipe recipe = RecipeFactory.create(true);
        recipe.setRecipeName("Festive Biryani");
        recipe.setRecipeCreatorId(creatorId);
        recipe = recipes.save(recipe);
        Long recipeId = recipe.getId();

        service.notifyNewRecipe(creatorId, recipe);

        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] msgs = greenMail.getReceivedMessages();
            assertThat(msgs).hasSize(2);
            assertThat(msgs[0].getContent().toString()).contains("Festive Biryani").contains("/recipes/" + recipeId);
            assertThat(msgs[1].getContent().toString()).contains("Festive Biryani").contains("/recipes/" + recipeId);
        });
    }
}

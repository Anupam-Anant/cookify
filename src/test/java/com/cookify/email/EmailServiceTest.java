package com.cookify.email;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;

@SpringBootTest
@TestPropertySource(properties = {"spring.mail.host=localhost", "spring.mail.port=3025"})
class EmailServiceTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired EmailService emailService;

    @Test
    void sendsOtpEmail() {
        emailService.sendOtp("aryan@example.com", "123456", "login");
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] msgs = greenMail.getReceivedMessages();
            assertThat(msgs).hasSize(1);
            assertThat(msgs[0].getSubject()).contains("code");
            assertThat(greenMail.getReceivedMessages()[0].getContent().toString()).contains("123456");
        });
    }
}

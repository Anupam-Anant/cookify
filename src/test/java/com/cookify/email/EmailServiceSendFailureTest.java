package com.cookify.email;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Plain Mockito unit test (no Spring context, same pattern as CommentServiceTest's mocked
 * collaborators): an SMTP outage must not propagate out of EmailService and crash whatever
 * request/background job triggered the email -- it should be logged and swallowed instead.
 */
class EmailServiceSendFailureTest {

    @Test
    void sendNotificationSwallowsMailSenderFailureInsteadOfThrowing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("Simulated SMTP outage"))
            .when(mailSender).send(any(SimpleMailMessage.class));
        EmailService emailService = new EmailService(mailSender);

        assertThatCode(() -> emailService.sendNotification("chef@example.com", "Subject", "Body"))
            .doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOtpSwallowsMailSenderFailureInsteadOfThrowing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("Simulated SMTP outage"))
            .when(mailSender).send(any(SimpleMailMessage.class));
        EmailService emailService = new EmailService(mailSender);

        assertThatCode(() -> emailService.sendOtp("chef@example.com", "123456", "login"))
            .doesNotThrowAnyException();
    }
}

package com.cookify.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String FROM = "no-reply@cookify.local";
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) { this.mailSender = mailSender; }

    @Async
    public void sendOtp(String toEmail, String code, String purposeLabel) {
        sendNotification(toEmail, "Your COOKify " + purposeLabel + " code",
            "Your 6-digit " + purposeLabel + " code is: " + code
            + "\nIt expires in 10 minutes. Do not share it.");
    }

    @Async
    public void sendPasswordResetCode(String toEmail, String code) {
        sendOtp(toEmail, code, "password reset");
    }

    @Async
    public void sendNotification(String toEmail, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(FROM);
        msg.setTo(toEmail);
        msg.setSubject(subject);
        msg.setText(body);
        log.info("Sending email to={} subject={}", toEmail, subject);
        try {
            mailSender.send(msg);
        } catch (Exception e) {
            // Never let an SMTP outage bubble out of here: the caller (comment rejection,
            // subscription notification, OTP dispatch, ...) must not fail just because mail
            // delivery did. Logged at ERROR so the outage is visible, not silent.
            log.error("Failed to send email to={} subject={}", toEmail, subject, e);
        }
    }
}

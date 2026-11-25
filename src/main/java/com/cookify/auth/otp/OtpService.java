package com.cookify.auth.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Matches the property default in application.yml; used by the 2-arg constructor
     *  (manual/test construction that doesn't go through Spring property injection). */
    public static final int DEFAULT_RESEND_COOLDOWN_SECONDS = 30;

    private final EmailOtpRepository repo;
    private final PasswordEncoder encoder;
    private final int resendCooldownSeconds;

    public OtpService(EmailOtpRepository repo, PasswordEncoder encoder) {
        this(repo, encoder, DEFAULT_RESEND_COOLDOWN_SECONDS);
    }

    @Autowired
    public OtpService(EmailOtpRepository repo, PasswordEncoder encoder,
                       @Value("${cookify.otp.resend-cooldown-seconds:30}") int resendCooldownSeconds) {
        this.repo = repo;
        this.encoder = encoder;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    @Transactional
    public String issue(Long userId, OtpPurpose purpose) {
        Optional<EmailOtp> lastIssued = repo.findFirstByUserIdAndPurposeOrderByIdDesc(userId, purpose);
        if (resendCooldownSeconds > 0 && lastIssued.isPresent()) {
            LocalDateTime cooldownEnd = lastIssued.get().getCreatedAt().plusSeconds(resendCooldownSeconds);
            if (cooldownEnd.isAfter(LocalDateTime.now())) {
                log.info("OTP issuance throttled userId={} purpose={}", userId, purpose);
                throw new OtpThrottledException("Please wait before requesting another code.");
            }
        }

        // Supersede: any prior unconsumed code for this (userId, purpose) becomes invalid the
        // moment a new one is issued -- only the newest code is ever allowed to verify.
        List<EmailOtp> priorUnconsumed = repo.findByUserIdAndPurposeAndConsumedFalse(userId, purpose);
        if (!priorUnconsumed.isEmpty()) {
            priorUnconsumed.forEach(o -> o.setConsumed(true));
            repo.saveAll(priorUnconsumed);
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailOtp otp = new EmailOtp();
        otp.setUserId(userId);
        otp.setPurpose(purpose);
        otp.setCodeHash(encoder.encode(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        repo.save(otp);
        // Never log the code itself -- only that one was issued.
        log.info("OTP issued userId={} purpose={}", userId, purpose);
        return code;
    }

    @Transactional
    public boolean verify(Long userId, OtpPurpose purpose, String code) {
        var found = repo.findFirstByUserIdAndPurposeAndConsumedFalseOrderByIdDesc(userId, purpose);
        if (found.isEmpty()) {
            log.info("OTP verify failed (no active otp) userId={} purpose={}", userId, purpose);
            return false;
        }
        EmailOtp otp = found.get();
        if (otp.getExpiresAt().isBefore(LocalDateTime.now()) || otp.getAttempts() >= MAX_ATTEMPTS) {
            log.info("OTP verify failed (expired or max attempts) userId={} purpose={}", userId, purpose);
            return false;
        }
        if (encoder.matches(code, otp.getCodeHash())) {
            otp.setConsumed(true);
            repo.save(otp);
            log.info("OTP verify succeeded userId={} purpose={}", userId, purpose);
            return true;
        }
        otp.setAttempts(otp.getAttempts() + 1);
        repo.save(otp);
        log.info("OTP verify failed (code mismatch) userId={} purpose={}", userId, purpose);
        return false;
    }
}

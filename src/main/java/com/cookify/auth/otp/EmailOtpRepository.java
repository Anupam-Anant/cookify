package com.cookify.auth.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findFirstByUserIdAndPurposeAndConsumedFalseOrderByIdDesc(Long userId, OtpPurpose purpose);

    /** All prior unconsumed codes for a (userId, purpose) pair -- superseded on reissue. */
    List<EmailOtp> findByUserIdAndPurposeAndConsumedFalse(Long userId, OtpPurpose purpose);

    /** Most recently issued code for a (userId, purpose) pair, regardless of consumed state --
     *  used to enforce the resend cooldown. */
    Optional<EmailOtp> findFirstByUserIdAndPurposeOrderByIdDesc(Long userId, OtpPurpose purpose);
}

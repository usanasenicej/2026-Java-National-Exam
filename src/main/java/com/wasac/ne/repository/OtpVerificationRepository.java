package com.wasac.ne.repository;

import com.wasac.ne.entity.OtpVerification;
import com.wasac.ne.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String email, OtpPurpose purpose);
}

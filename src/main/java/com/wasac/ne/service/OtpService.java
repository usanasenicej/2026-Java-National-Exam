package com.wasac.ne.service;

import com.wasac.ne.config.OtpProperties;
import com.wasac.ne.entity.OtpVerification;
import com.wasac.ne.enums.OtpPurpose;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final OtpProperties otpProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateAndSaveOtp(String email, OtpPurpose purpose) {
        String otp = generateOtp();
        OtpVerification verification = OtpVerification.builder()
                .email(email.toLowerCase())
                .otpCode(otp)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()))
                .used(false)
                .build();
        otpRepository.save(verification);
        return otp;
    }

    @Transactional
    public void verifyOtp(String email, String otpCode, OtpPurpose purpose) {
        OtpVerification otp = otpRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email.toLowerCase(), purpose)
                .orElseThrow(() -> new BusinessException("No valid OTP found for this email. Please request a new one."));

        if (otp.isUsed()) {
            throw new BusinessException("OTP has already been used. Please request a new one.");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP has expired. Please request a new one.");
        }
        if (!otp.getOtpCode().equals(otpCode)) {
            throw new BusinessException("Invalid OTP code. Please check and try again.");
        }

        otp.setUsed(true);
        otpRepository.save(otp);
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpProperties.getLength());
        int otp = secureRandom.nextInt(bound / 10, bound);
        return String.valueOf(otp);
    }
}

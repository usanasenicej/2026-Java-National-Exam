package com.wasac.ne.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new com.wasac.ne.exception.BusinessException(
                    "Failed to send email. Please verify your Gmail app password in application.properties");
        }
    }

    public void sendWelcomeEmail(String to, String name) {
        sendEmail(to, "Welcome to WASAC Utility Billing",
                "Dear " + name + ",\n\nWelcome to the WASAC Utility Billing System. Your account has been created successfully.\n\nRegards,\nWASAC Team");
    }

    public void sendOtpEmail(String to, String name, String otp, String purpose) {
        sendEmail(to, "WASAC - Your OTP Code",
                "Dear " + name + ",\n\nYour OTP for " + purpose + " is: " + otp +
                        "\n\nThis code expires in 10 minutes. Do not share it with anyone.\n\nRegards,\nWASAC Team");
    }

    public void sendBillNotificationEmail(String to, String name, String message) {
        sendEmail(to, "WASAC - Utility Bill Notification", message);
    }

    public void sendPasswordResetEmail(String to, String name, String otp) {
        sendOtpEmail(to, name, otp, "password reset");
    }
}

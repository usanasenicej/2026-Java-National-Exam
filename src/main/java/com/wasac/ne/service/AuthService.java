package com.wasac.ne.service;

import com.wasac.ne.config.JwtProperties;
import com.wasac.ne.dto.request.*;
import com.wasac.ne.dto.response.AuthResponse;
import com.wasac.ne.entity.BlacklistedToken;
import com.wasac.ne.entity.Customer;
import com.wasac.ne.entity.EmailVerificationToken;
import com.wasac.ne.entity.PasswordResetToken;
import com.wasac.ne.entity.RefreshToken;
import com.wasac.ne.entity.User;
import com.wasac.ne.enums.OtpPurpose;
import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.UserRole;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.exception.UnauthorizedException;
import com.wasac.ne.repository.BlacklistedTokenRepository;
import com.wasac.ne.repository.CustomerRepository;
import com.wasac.ne.repository.EmailVerificationTokenRepository;
import com.wasac.ne.repository.PasswordResetTokenRepository;
import com.wasac.ne.repository.RefreshTokenRepository;
import com.wasac.ne.repository.UserRepository;
import com.wasac.ne.security.JwtService;
import com.wasac.ne.security.SecurityUtils;
import com.wasac.ne.validation.ValidEmailValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final JwtProperties jwtProperties;
    private final ValidEmailValidator emailValidator = new ValidEmailValidator();

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public void register(RegisterRequest request) {
        String[] nameParts = request.getFullNames().trim().split("\\s+");
        if (nameParts.length < 2) {
            throw new BusinessException("Full names must contain both a first name and a last name");
        }

        validateEmailCrossFields(request.getEmail(), request.getFullNames(),
                request.getPassword(), request.getPhoneNumber());

        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BusinessException("A user with email '" + request.getEmail() + "' already exists");
        }

        // Validate National ID uniqueness
        if (userRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessException("An account with National ID '" + request.getNationalId() + "' already exists");
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.ROLE_CUSTOMER;
        if (role != UserRole.ROLE_CUSTOMER) {
            throw new BusinessException("Self-registration is only allowed for ROLE_CUSTOMER. Contact admin for other roles.");
        }

        // Validate age if dateOfBirth is provided
        if (request.getDateOfBirth() != null) {
            int age = java.time.Period.between(request.getDateOfBirth(), java.time.LocalDate.now()).getYears();
            if (age < 18) {
                throw new BusinessException("You must be at least 18 years old to register. Current age: " + age);
            }
        }

        // Auto-create the customer profile first
        if (customerRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessException("A customer profile with National ID '" + request.getNationalId() + "' already exists");
        }
        if (customerRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BusinessException("A customer profile with email '" + request.getEmail() + "' already exists");
        }
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("A customer profile with phone '" + request.getPhoneNumber() + "' already exists");
        }

        Customer customer = Customer.builder()
                .fullNames(request.getFullNames().trim())
                .nationalId(request.getNationalId())
                .email(request.getEmail().toLowerCase())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress().trim())
                .dateOfBirth(request.getDateOfBirth())
                .status(Status.ACTIVE)
                .build();
        customer = customerRepository.save(customer);

        // Create the user account, linked to the customer profile
        User user = User.builder()
                .fullNames(request.getFullNames().trim())
                .nationalId(request.getNationalId())
                .email(request.getEmail().toLowerCase())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(Status.ACTIVE)
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .emailVerified(false)
                .customer(customer)
                .build();

        user = userRepository.save(user);
        auditService.log("User", user.getId(), "REGISTER", "User registered, pending verification");

        String otp = otpService.generateAndSaveOtp(user.getEmail(), OtpPurpose.REGISTRATION);

        String tokenStr = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(tokenStr)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        String link = baseUrl + "/api/auth/verify?token=" + tokenStr;

        // Send dedicated registration email — OTP displayed prominently + verification link
        emailService.sendRegistrationVerificationEmail(user.getEmail(), user.getFullNames(), otp, link);
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        otpService.verifyOtp(request.getEmail(), request.getOtpCode(), OtpPurpose.REGISTRATION);

        user.setEmailVerified(true);
        userRepository.save(user);
        auditService.log("User", user.getId(), "VERIFY_OTP", "Email verified via OTP");
        emailService.sendAccountVerifiedEmail(user.getEmail(), user.getFullNames());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!user.isEmailVerified()) {
            throw new BusinessException("Email not verified. Please verify your account using the OTP sent to your email.");
        }
        if (user.getStatus() == Status.INACTIVE) {
            throw new BusinessException("Your account is inactive. Please contact the administrator.");
        }
        if (user.getStatus() == Status.SUSPENDED) {
            throw new BusinessException("Your account is suspended. Please contact the administrator.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword()));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired. Please login again.");
        }

        User user = refreshToken.getUser();
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("No access token provided in Authorization header");
        }

        String token = authHeader.substring(7);
        if (blacklistedTokenRepository.existsByToken(token)) {
            throw new BusinessException("Token has already been invalidated");
        }

        LocalDateTime expiresAt = jwtService.extractExpiration(token)
                .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

        blacklistedTokenRepository.save(BlacklistedToken.builder()
                .token(token)
                .expiresAt(expiresAt)
                .blacklistedAt(LocalDateTime.now())
                .build());

        String email = SecurityUtils.getCurrentUserEmail();
        userRepository.findByEmail(email).ifPresent(user -> {
            refreshTokenRepository.deleteByUserId(user.getId());
            auditService.log("User", user.getId(), "LOGOUT", "User signed out");
        });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        String otp = otpService.generateAndSaveOtp(user.getEmail(), OtpPurpose.PASSWORD_RESET);

        String tokenStr = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(tokenStr)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(2))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        String resetLink = baseUrl + "/api/auth/reset-password-page?token=" + tokenStr;

        // Send OTP email first
        emailService.sendOtpEmail(user.getEmail(), user.getFullNames(), otp, "password reset");
        // Also send the reset link separately as plain fallback
        emailService.sendEmail(user.getEmail(), "WASAC - Password Reset Link",
                "Dear " + user.getFullNames() + ",\n\nYou can also reset your password using this link:\n" + resetLink + "\n\nThis link expires in 2 hours.\n\nWASAC Team");
        auditService.log("User", user.getId(), "FORGOT_PASSWORD", "Password reset OTP and link sent");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        otpService.verifyOtp(request.getEmail(), request.getOtpCode(), OtpPurpose.PASSWORD_RESET);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(user.getId());
        auditService.log("User", user.getId(), "RESET_PASSWORD", "Password reset successfully");
    }

    @Transactional
    public void verifyLink(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid or expired verification link"));

        if (verificationToken.isUsed()) {
            throw new BusinessException("This verification link has already been used");
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("This verification link has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        auditService.log("User", user.getId(), "VERIFY_LINK", "Email verified via link");
        emailService.sendAccountVerifiedEmail(user.getEmail(), user.getFullNames());
    }

    @Transactional
    public void resetPasswordWithLink(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid or expired password reset link"));

        if (resetToken.isUsed()) {
            throw new BusinessException("This password reset link has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("This password reset link has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.deleteByUserId(user.getId());
        auditService.log("User", user.getId(), "RESET_PASSWORD_LINK", "Password reset successfully via link");
    }

    @Transactional
    public void resendRegistrationOtp(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        if (user.isEmailVerified()) {
            throw new BusinessException("Account is already verified. Please login.");
        }

        String otp = otpService.generateAndSaveOtp(user.getEmail(), OtpPurpose.REGISTRATION);
        emailService.sendOtpEmail(user.getEmail(), user.getFullNames(), otp, "account verification");
    }

    @Transactional
    public com.wasac.ne.dto.response.UserResponse getCurrentUser() {
        User user = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        return com.wasac.ne.mapper.EntityMapper.toUserResponse(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password must be different from your current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(user.getId());
        auditService.log("User", user.getId(), "CHANGE_PASSWORD", "Password changed successfully");
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenStr = jwtService.generateRefreshToken(user);

        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000))
                .revoked(false)
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullNames(user.getFullNames())
                .roles(user.getRoles())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    private void validateEmailCrossFields(String email, String fullNames, String password, String phone) {
        emailValidator.setCrossFieldValues(fullNames, password, phone);
        if (!emailValidator.isValid(email, null)) {
            throw new BusinessException("Invalid email: email must be lowercase, valid format, and cannot match name, password, or phone number");
        }
    }
}

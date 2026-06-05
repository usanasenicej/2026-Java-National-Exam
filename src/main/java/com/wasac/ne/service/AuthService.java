package com.wasac.ne.service;

import com.wasac.ne.config.JwtProperties;
import com.wasac.ne.dto.request.*;
import com.wasac.ne.dto.response.AuthResponse;
import com.wasac.ne.entity.BlacklistedToken;
import com.wasac.ne.entity.RefreshToken;
import com.wasac.ne.entity.User;
import com.wasac.ne.enums.OtpPurpose;
import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.UserRole;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.exception.UnauthorizedException;
import com.wasac.ne.repository.BlacklistedTokenRepository;
import com.wasac.ne.repository.RefreshTokenRepository;
import com.wasac.ne.repository.UserRepository;
import com.wasac.ne.security.JwtService;
import com.wasac.ne.security.SecurityUtils;
import com.wasac.ne.validation.ValidEmailValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final JwtProperties jwtProperties;
    private final ValidEmailValidator emailValidator = new ValidEmailValidator();

    @Transactional
    public void register(RegisterRequest request) {
        validateEmailCrossFields(request.getEmail(), request.getFullNames(),
                request.getPassword(), request.getPhoneNumber());

        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BusinessException("A user with email '" + request.getEmail() + "' already exists");
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.ROLE_CUSTOMER;
        if (role != UserRole.ROLE_CUSTOMER) {
            throw new BusinessException("Self-registration is only allowed for ROLE_CUSTOMER. Contact admin for other roles.");
        }

        User user = User.builder()
                .fullNames(request.getFullNames().trim())
                .email(request.getEmail().toLowerCase())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(Status.ACTIVE)
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        auditService.log("User", user.getId(), "REGISTER", "User registered, pending OTP verification");

        String otp = otpService.generateAndSaveOtp(user.getEmail(), OtpPurpose.REGISTRATION);
        emailService.sendOtpEmail(user.getEmail(), user.getFullNames(), otp, "account verification");
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullNames());
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        otpService.verifyOtp(request.getEmail(), request.getOtpCode(), OtpPurpose.REGISTRATION);

        user.setEmailVerified(true);
        userRepository.save(user);
        auditService.log("User", user.getId(), "VERIFY_OTP", "Email verified via OTP");
        emailService.sendEmail(user.getEmail(), "Account Verified",
                "Dear " + user.getFullNames() + ",\n\nYour WASAC account has been verified. You can now log in.\n\nRegards,\nWASAC Team");
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
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullNames(), otp);
        auditService.log("User", user.getId(), "FORGOT_PASSWORD", "Password reset OTP sent");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        otpService.verifyOtp(request.getEmail(), request.getOtpCode(), OtpPurpose.PASSWORD_RESET);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(user.getId());
        auditService.log("User", user.getId(), "RESET_PASSWORD", "Password reset successfully");
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
                .build();
    }

    private void validateEmailCrossFields(String email, String fullNames, String password, String phone) {
        emailValidator.setCrossFieldValues(fullNames, password, phone);
        if (!emailValidator.isValid(email, null)) {
            throw new BusinessException("Invalid email: email must be lowercase, valid format, and cannot match name, password, or phone number");
        }
    }
}

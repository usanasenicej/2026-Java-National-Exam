package com.wasac.ne.controller;

import com.wasac.ne.dto.request.*;
import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.AuthResponse;
import com.wasac.ne.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public authentication endpoints — no JWT required")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Public. Sends OTP to email for verification. Default role: ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please verify your email using the OTP sent."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify registration OTP", description = "Public. Activates account after OTP verification")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You can now login."));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Public. Returns JWT access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Public. Provide valid refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out", description = "Authenticated. Blacklists current JWT token. Use Authorize button with Bearer token.")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Signed out successfully. Token has been invalidated."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Public. Sends OTP to registered email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP sent to your email."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Public. Verify OTP and set new password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. Please login with your new password."));
    }
}

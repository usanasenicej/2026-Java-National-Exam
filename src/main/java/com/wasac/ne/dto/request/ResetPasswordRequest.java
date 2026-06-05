package com.wasac.ne.dto.request;

import com.wasac.ne.validation.StrongPassword;
import com.wasac.ne.validation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Reset password using OTP")
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits")
    private String otpCode;

    @NotBlank(message = "New password is required")
    @StrongPassword
    private String newPassword;
}

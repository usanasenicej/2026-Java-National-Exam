package com.wasac.ne.dto.request;

import com.wasac.ne.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Reset password using verification link")
public class ResetPasswordLinkRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @StrongPassword
    private String newPassword;
}

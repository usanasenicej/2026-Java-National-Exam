package com.wasac.ne.dto.request;

import com.wasac.ne.validation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request. Public endpoint.")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

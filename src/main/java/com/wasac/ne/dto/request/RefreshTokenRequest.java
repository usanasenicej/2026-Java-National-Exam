package com.wasac.ne.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Refresh access token")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}

package com.wasac.ne.dto.request;

import com.wasac.ne.enums.UserRole;
import com.wasac.ne.validation.StrongPassword;
import com.wasac.ne.validation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User registration request. Public endpoint — no authentication required.")
public class RegisterRequest {

    @NotBlank(message = "Full names are required")
    @Size(min = 2, max = 100, message = "Full names must be between 2 and 100 characters")
    private String fullNames;

    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?250[0-9]{9}$|^0[0-9]{9}$", message = "Phone must be a valid Rwanda number (e.g. +250788123456 or 0788123456)")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    @NotNull(message = "Role is required")
    @Schema(description = "Default role for self-registration is ROLE_CUSTOMER. Other roles require ADMIN.")
    private UserRole role;
}

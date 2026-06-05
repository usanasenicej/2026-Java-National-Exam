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
@Schema(description = "Customer self-registration request. Public endpoint — no authentication required.")
public class RegisterRequest {

    @NotBlank(message = "Full names are required")
    @Size(min = 2, max = 100, message = "Full names must be between 2 and 100 characters")
    private String fullNames;

    @NotBlank(message = "National ID is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "National ID must be exactly 16 digits")
    @Schema(description = "Rwanda National ID — exactly 16 digits, must be unique in the system")
    private String nationalId;

    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?2507[2389]|07[2389])[0-9]{7}$",
             message = "Phone must be a valid Rwanda number (e.g. +250788123456 or 0788123456)")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    @Schema(description = "Physical address (e.g. KG 15 Ave, Kigali)")
    private String address;

    @Schema(description = "Date of birth (optional, must be 18+ years old if provided)")
    private java.time.LocalDate dateOfBirth;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    @NotNull(message = "Role is required")
    @Schema(description = "Must be ROLE_CUSTOMER for self-registration. Other roles require Admin.")
    private UserRole role;
}

package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.UserRole;
import com.wasac.ne.validation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "Create user — ROLE_ADMIN only. Password is optional; a temporary password is auto-generated for staff roles.")
public class CreateUserRequest {

    @NotBlank(message = "Full names are required")
    @Size(min = 2, max = 100)
    private String fullNames;

    @NotBlank @ValidEmail
    private String email;

    @NotBlank
    @Pattern(regexp = "^(\\+?2507[2389]|07[2389])[0-9]{7}$", message = "Phone must be a valid Rwanda number (e.g. +250788123456 or 0788123456)")
    private String phoneNumber;

    @Schema(description = "Optional. If omitted for Operator/Finance/Admin roles, a temporary password is generated and emailed.")
    private String password;

    @NotNull
    private Status status;

    @NotEmpty(message = "At least one role is required")
    private Set<UserRole> roles;

    private Long customerId;
}

package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "Update user — ROLE_ADMIN only")
public class UpdateUserRequest {

    @Size(min = 2, max = 100)
    private String fullNames;

    @Pattern(regexp = "^\\+?250[0-9]{9}$|^0[0-9]{9}$", message = "Phone must be a valid Rwanda number")
    private String phoneNumber;

    private Status status;

    private Set<UserRole> roles;

    private Long customerId;
}

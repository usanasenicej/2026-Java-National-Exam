package com.wasac.ne.dto.response;

import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String fullNames;
    private String nationalId;
    private String email;
    private String phoneNumber;
    private Status status;
    private Set<UserRole> roles;
    private boolean emailVerified;
    private boolean mustChangePassword;
    private Long customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

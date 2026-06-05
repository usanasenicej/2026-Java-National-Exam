package com.wasac.ne.dto.response;

import com.wasac.ne.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String fullNames;
    private Set<UserRole> roles;
    private boolean mustChangePassword;
}

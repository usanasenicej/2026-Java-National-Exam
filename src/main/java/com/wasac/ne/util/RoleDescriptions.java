package com.wasac.ne.util;

import com.wasac.ne.enums.UserRole;

import java.util.Set;
import java.util.stream.Collectors;

public final class RoleDescriptions {

    private RoleDescriptions() {
    }

    public static String describeRoles(Set<UserRole> roles) {
        return roles.stream()
                .map(RoleDescriptions::describeRole)
                .collect(Collectors.joining("\n- ", "- ", ""));
    }

    public static String describeRole(UserRole role) {
        return switch (role) {
            case ROLE_ADMIN -> "ROLE_ADMIN: Configure tariffs, approve bills, and manage system users";
            case ROLE_OPERATOR -> "ROLE_OPERATOR: Capture and manage meter readings for customers";
            case ROLE_FINANCE -> "ROLE_FINANCE: Approve bills, record payments, and manage billing operations";
            case ROLE_CUSTOMER -> "ROLE_CUSTOMER: View personal bills and payment history";
        };
    }

    public static String formatRoleNames(Set<UserRole> roles) {
        return roles.stream().map(Enum::name).collect(Collectors.joining(", "));
    }
}

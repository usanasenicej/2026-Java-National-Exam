package com.wasac.ne.controller;

import com.wasac.ne.dto.request.CreateUserRequest;
import com.wasac.ne.dto.request.UpdateUserRequest;
import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.dto.response.UserResponse;
import com.wasac.ne.enums.Status;
import com.wasac.ne.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "User Management", description = "Manage system users — ROLE_ADMIN only")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Access: ROLE_ADMIN. Auto-generates temporary password and emails credentials for Operator/Finance/Admin roles.")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created", userService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved", userService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users", description = "Access: ROLE_ADMIN. Supports search and pagination")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved",
                userService.getAll(search, status, PageRequest.of(page, size, sort))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Access: ROLE_ADMIN. Sends email notification when roles are changed.")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }

    @PostMapping("/{id}/resend-otp")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resend verification OTP", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@PathVariable Long id) {
        userService.resendVerificationOtp(id);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP resent"));
    }
}

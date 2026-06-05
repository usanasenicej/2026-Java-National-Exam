package com.wasac.ne.controller;

import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.NotificationResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Notifications", description = "Customer billing notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "List all notifications", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved",
                notificationService.getAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get customer notifications", description = "Access: ROLE_ADMIN, ROLE_FINANCE, ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getByCustomer(
            @PathVariable Long customerId,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved",
                notificationService.getByCustomer(customerId, read, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Mark notification as read", description = "Access: ROLE_ADMIN, ROLE_FINANCE, ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", notificationService.markAsRead(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete notification", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted"));
    }
}

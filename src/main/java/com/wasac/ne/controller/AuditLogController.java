package com.wasac.ne.controller;

import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.AuditLogResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.service.AuditLogService;
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
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Audit Logs", description = "System audit trail")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List audit logs", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved",
                auditLogService.getAll(search, PageRequest.of(page, size, Sort.by("timestamp").descending()))));
    }
}

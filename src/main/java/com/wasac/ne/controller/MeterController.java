package com.wasac.ne.controller;

import com.wasac.ne.dto.request.CreateMeterRequest;
import com.wasac.ne.dto.request.UpdateMeterRequest;
import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.MeterResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.service.MeterService;
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
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Meter Management", description = "Manage customer utility meters")
public class MeterController {

    private final MeterService meterService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Create meter", description = "Access: ROLE_ADMIN, ROLE_OPERATOR")
    public ResponseEntity<ApiResponse<MeterResponse>> create(@Valid @RequestBody CreateMeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter created", meterService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get meter", description = "Access: All authenticated roles")
    public ResponseEntity<ApiResponse<MeterResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Meter retrieved", meterService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "List meters", description = "Access: ROLE_ADMIN, ROLE_OPERATOR, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<MeterResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) MeterType meterType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Meters retrieved",
                meterService.getAll(search, customerId, meterType, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Update meter", description = "Access: ROLE_ADMIN, ROLE_OPERATOR")
    public ResponseEntity<ApiResponse<MeterResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateMeterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Meter updated", meterService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete meter", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        meterService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Meter deleted"));
    }
}

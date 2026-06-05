package com.wasac.ne.controller;

import com.wasac.ne.dto.request.CreateMeterReadingRequest;
import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.MeterReadingResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.service.MeterReadingService;
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
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Meter Readings", description = "Capture and manage meter readings")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Capture meter reading", description = "Access: ROLE_OPERATOR, ROLE_ADMIN. One reading per meter per month.")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> create(@Valid @RequestBody CreateMeterReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter reading captured", meterReadingService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get reading", description = "Access: ROLE_ADMIN, ROLE_OPERATOR, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Reading retrieved", meterReadingService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "List readings", description = "Access: ROLE_ADMIN, ROLE_OPERATOR, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<MeterReadingResponse>>> getAll(
            @RequestParam(required = false) Long meterId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Readings retrieved",
                meterReadingService.getAll(meterId, month, year, PageRequest.of(page, size, Sort.by("readingDate").descending()))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete reading", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        meterReadingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Reading deleted"));
    }
}

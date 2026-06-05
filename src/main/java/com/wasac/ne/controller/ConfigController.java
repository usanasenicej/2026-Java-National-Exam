package com.wasac.ne.controller;

import com.wasac.ne.dto.request.CreatePenaltyConfigRequest;
import com.wasac.ne.dto.request.CreateServiceChargeRequest;
import com.wasac.ne.dto.request.CreateTaxConfigRequest;
import com.wasac.ne.dto.request.UpdatePenaltyConfigRequest;
import com.wasac.ne.dto.request.UpdateServiceChargeRequest;
import com.wasac.ne.dto.request.UpdateTaxConfigRequest;
import com.wasac.ne.dto.response.*;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Tax, Service Charge & Penalty", description = "Billing configuration — ROLE_ADMIN")
public class ConfigController {

    private final ConfigService configService;

    // ── Service Charges ────────────────────────────────────────────────────

    @PostMapping("/service-charges")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create service charge", description = "Access: ROLE_ADMIN. Auto-increments version.")
    public ResponseEntity<ApiResponse<ServiceChargeResponse>> createServiceCharge(
            @Valid @RequestBody CreateServiceChargeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service charge created", configService.createServiceCharge(request)));
    }

    @GetMapping("/service-charges")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "List service charges", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<ServiceChargeResponse>>> getServiceCharges(
            @RequestParam(required = false) MeterType meterType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Service charges retrieved",
                configService.getServiceCharges(meterType, PageRequest.of(page, size))));
    }

    @PutMapping("/service-charges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update service charge",
        description = "Access: ROLE_ADMIN. Updates name, amount, effectiveTo, or status. Only provided fields are changed.")
    public ResponseEntity<ApiResponse<ServiceChargeResponse>> updateServiceCharge(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceChargeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Service charge updated",
                configService.updateServiceCharge(id, request)));
    }

    @DeleteMapping("/service-charges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete service charge", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> deleteServiceCharge(@PathVariable Long id) {
        configService.deleteServiceCharge(id);
        return ResponseEntity.ok(ApiResponse.success("Service charge deleted"));
    }

    // ── Tax Configs ────────────────────────────────────────────────────────

    @PostMapping("/taxes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create tax config", description = "Access: ROLE_ADMIN. Auto-increments version.")
    public ResponseEntity<ApiResponse<TaxConfigResponse>> createTax(
            @Valid @RequestBody CreateTaxConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tax config created", configService.createTax(request)));
    }

    @GetMapping("/taxes")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "List tax configs", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<TaxConfigResponse>>> getTaxes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Tax configs retrieved",
                configService.getTaxes(PageRequest.of(page, size))));
    }

    @PutMapping("/taxes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tax config",
        description = "Access: ROLE_ADMIN. Updates name, percentage (0-100), effectiveTo, or status. Only provided fields are changed.")
    public ResponseEntity<ApiResponse<TaxConfigResponse>> updateTax(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaxConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tax config updated", configService.updateTax(id, request)));
    }

    @DeleteMapping("/taxes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete tax config", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> deleteTax(@PathVariable Long id) {
        configService.deleteTax(id);
        return ResponseEntity.ok(ApiResponse.success("Tax config deleted"));
    }

    // ── Penalty Configs ────────────────────────────────────────────────────

    @PostMapping("/penalties")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create penalty config", description = "Access: ROLE_ADMIN. Auto-increments version.")
    public ResponseEntity<ApiResponse<PenaltyConfigResponse>> createPenalty(
            @Valid @RequestBody CreatePenaltyConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Penalty config created", configService.createPenalty(request)));
    }

    @GetMapping("/penalties")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "List penalty configs", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<PenaltyConfigResponse>>> getPenalties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Penalty configs retrieved",
                configService.getPenalties(PageRequest.of(page, size))));
    }

    @PutMapping("/penalties/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update penalty config",
        description = "Access: ROLE_ADMIN. Updates name, percentage, gracePeriodDays, effectiveTo, or status. Only provided fields are changed.")
    public ResponseEntity<ApiResponse<PenaltyConfigResponse>> updatePenalty(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePenaltyConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Penalty config updated",
                configService.updatePenalty(id, request)));
    }

    @DeleteMapping("/penalties/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete penalty config", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> deletePenalty(@PathVariable Long id) {
        configService.deletePenalty(id);
        return ResponseEntity.ok(ApiResponse.success("Penalty config deleted"));
    }
}

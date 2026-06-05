package com.wasac.ne.controller;

import com.wasac.ne.dto.request.CreateTariffRequest;
import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.dto.response.TariffResponse;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.service.TariffService;
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
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Tariff Configuration", description = "Versioned consumption tariffs — ROLE_ADMIN")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create tariff", description = "Access: ROLE_ADMIN. New version applies to future billing cycles only.")
    public ResponseEntity<ApiResponse<TariffResponse>> create(@Valid @RequestBody CreateTariffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tariff created", tariffService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get tariff", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<TariffResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tariff retrieved", tariffService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "List tariffs", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<TariffResponse>>> getAll(
            @RequestParam(required = false) MeterType meterType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Tariffs retrieved",
                tariffService.getAll(meterType, PageRequest.of(page, size, Sort.by("version").descending()))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete tariff", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tariffService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Tariff deleted"));
    }
}

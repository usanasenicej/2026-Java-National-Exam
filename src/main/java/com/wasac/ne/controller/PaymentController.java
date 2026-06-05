package com.wasac.ne.controller;

import com.wasac.ne.dto.request.CreatePaymentRequest;
import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.dto.response.PaymentResponse;
import com.wasac.ne.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Payments", description = "Record and manage bill payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Record payment", description = "Access: ROLE_ADMIN, ROLE_FINANCE. Supports partial and full payments.")
    public ResponseEntity<ApiResponse<PaymentResponse>> create(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded", paymentService.recordPayment(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get payment", description = "Access: ROLE_ADMIN, ROLE_FINANCE, ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", paymentService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "List payments", description = "Access: ROLE_ADMIN, ROLE_FINANCE, ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getAll(
            @RequestParam(required = false) Long billId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved",
                paymentService.getAll(billId, search, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete payment", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted"));
    }
}

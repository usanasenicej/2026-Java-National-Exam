package com.wasac.ne.controller;

import com.wasac.ne.dto.request.GenerateBillRequest;
import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.BillResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.enums.BillStatus;
import com.wasac.ne.service.BillService;
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
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Billing", description = "Bill generation and approval")
public class BillController {

    private final BillService billService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Generate bill from reading", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<BillResponse>> generate(@Valid @RequestBody GenerateBillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bill generated", billService.generateBill(request)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Approve bill", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<BillResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Bill approved", billService.approveBill(id)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Reject bill", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<BillResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Bill rejected", billService.rejectBill(id)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get bill", description = "Access: ROLE_ADMIN, ROLE_FINANCE, ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<BillResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved", billService.getById(id)));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get bill by reference", description = "Access: ROLE_ADMIN, ROLE_FINANCE, ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<BillResponse>> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved", billService.getByReference(reference)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "List bills", description = "Access: ROLE_ADMIN, ROLE_FINANCE, ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<PageResponse<BillResponse>>> getAll(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved",
                billService.getAll(customerId, status, search, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete bill", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        billService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Bill deleted"));
    }

    @PostMapping("/process-overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
        summary = "Process overdue bills",
        description = "Access: ROLE_ADMIN, ROLE_FINANCE. " +
            "Marks APPROVED bills past their due date as OVERDUE and applies late payment penalties. " +
            "Bills overdue for more than 90 days trigger automatic meter disconnection."
    )
    public ResponseEntity<ApiResponse<String>> processOverdue() {
        int count = billService.processOverdueBills();
        return ResponseEntity.ok(ApiResponse.success(
                "Overdue processing complete. Bills updated: " + count));
    }
}

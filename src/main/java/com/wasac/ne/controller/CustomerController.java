package com.wasac.ne.controller;

import com.wasac.ne.dto.request.CreateCustomerRequest;
import com.wasac.ne.dto.request.UpdateCustomerRequest;
import com.wasac.ne.dto.response.ApiResponse;
import com.wasac.ne.dto.response.CustomerResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.enums.Status;
import com.wasac.ne.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Customer Management", description = "Manage utility customers — Admin manages records, Customers self-register via /api/auth/register")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create customer record",
        description = "Access: ROLE_ADMIN only. Creates a customer profile that can then be linked to a user account and assigned meters. " +
            "Note: Customers self-register via POST /api/auth/register — this endpoint is for admin to create the customer profile manually.")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created", customerService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get customer", description = "Access: ROLE_ADMIN, ROLE_FINANCE, ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved", customerService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "List customers", description = "Access: ROLE_ADMIN, ROLE_FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved",
                customerService.getAll(search, status, PageRequest.of(page, size, sort))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update customer", description = "Access: ROLE_ADMIN only")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated", customerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete customer", description = "Access: ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted"));
    }
}

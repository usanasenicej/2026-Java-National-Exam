package com.wasac.ne.dto.request;

import com.wasac.ne.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Record payment — ROLE_FINANCE, ROLE_ADMIN")
public class CreatePaymentRequest {

    @NotBlank(message = "Bill reference is required")
    private String billReference;

    @NotNull @DecimalMin(value = "0.01", message = "Amount paid must be greater than zero")
    private BigDecimal amountPaid;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDate paymentDate;
}

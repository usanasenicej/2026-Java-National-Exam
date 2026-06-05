package com.wasac.ne.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Capture meter reading — ROLE_OPERATOR")
public class CreateMeterReadingRequest {

    @NotNull(message = "Meter ID is required")
    private Long meterId;

    @NotNull @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal previousReading;

    @NotNull @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal currentReading;

    @NotNull @PastOrPresent(message = "Reading date cannot be in the future")
    private LocalDate readingDate;
}

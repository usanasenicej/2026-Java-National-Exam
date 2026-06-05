package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTaxConfigRequest {

    @NotBlank
    private String name;

    @NotNull @DecimalMin("0.0")
    private BigDecimal percentage;

    @NotNull
    private LocalDate effectiveFrom;

    @NotNull
    private Status status;
}

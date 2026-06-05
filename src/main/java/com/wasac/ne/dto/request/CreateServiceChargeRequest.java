package com.wasac.ne.dto.request;

import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateServiceChargeRequest {

    @NotBlank
    private String name;

    @NotNull
    private MeterType meterType;

    @NotNull @DecimalMin("0.0")
    private BigDecimal amount;

    @NotNull
    private LocalDate effectiveFrom;

    @NotNull
    private Status status;
}

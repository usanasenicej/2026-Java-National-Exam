package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreatePenaltyConfigRequest {

    @NotBlank
    private String name;

    @NotNull @DecimalMin("0.0")
    private BigDecimal percentage;

    @NotNull @Min(0)
    private Integer gracePeriodDays;

    @NotNull
    private LocalDate effectiveFrom;

    @NotNull
    private Status status;
}

package com.wasac.ne.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TariffTierRequest {

    @NotNull @Min(1)
    private Integer tierOrder;

    @NotNull @DecimalMin("0.0")
    private BigDecimal minUnits;

    private BigDecimal maxUnits;

    @NotNull @DecimalMin("0.0")
    private BigDecimal ratePerUnit;
}

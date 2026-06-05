package com.wasac.ne.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TariffTierResponse {

    private Long id;
    private int tierOrder;
    private BigDecimal minUnits;
    private BigDecimal maxUnits;
    private BigDecimal ratePerUnit;
}

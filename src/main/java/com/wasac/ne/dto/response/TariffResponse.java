package com.wasac.ne.dto.response;

import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.TariffType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TariffResponse {

    private Long id;
    private String name;
    private MeterType meterType;
    private TariffType tariffType;
    private int version;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal flatRate;
    private Status status;
    private List<TariffTierResponse> tiers;
    private LocalDateTime createdAt;
}

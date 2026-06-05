package com.wasac.ne.dto.response;

import com.wasac.ne.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PenaltyConfigResponse {

    private Long id;
    private String name;
    private BigDecimal percentage;
    private int gracePeriodDays;
    private int version;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Status status;
    private LocalDateTime createdAt;
}

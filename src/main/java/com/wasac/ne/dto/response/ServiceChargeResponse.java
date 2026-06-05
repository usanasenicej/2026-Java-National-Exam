package com.wasac.ne.dto.response;

import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceChargeResponse {

    private Long id;
    private String name;
    private MeterType meterType;
    private BigDecimal amount;
    private int version;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Status status;
    private LocalDateTime createdAt;
}

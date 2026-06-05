package com.wasac.ne.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MeterReadingResponse {

    private Long id;
    private Long meterId;
    private String meterNumber;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private BigDecimal consumption;
    private LocalDate readingDate;
    private int readingMonth;
    private int readingYear;
    private LocalDateTime createdAt;
}

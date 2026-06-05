package com.wasac.ne.dto.response;

import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MeterResponse {

    private Long id;
    private String meterNumber;
    private MeterType meterType;
    private LocalDate installationDate;
    private Status status;
    private Long customerId;
    private String customerName;
    private LocalDateTime createdAt;
}

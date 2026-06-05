package com.wasac.ne.dto.request;

import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateMeterRequest {

    private MeterType meterType;

    @PastOrPresent(message = "Installation date cannot be in the future")
    private LocalDate installationDate;

    private Status status;
}

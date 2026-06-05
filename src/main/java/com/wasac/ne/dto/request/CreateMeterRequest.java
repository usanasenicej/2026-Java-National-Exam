package com.wasac.ne.dto.request;

import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Create meter for a customer")
public class CreateMeterRequest {

    @NotBlank(message = "Meter number is required")
    private String meterNumber;

    @NotNull
    private MeterType meterType;

    @NotNull @PastOrPresent(message = "Installation date cannot be in the future")
    private LocalDate installationDate;

    @NotNull
    private Status status;

    @NotNull(message = "Customer ID is required")
    private Long customerId;
}

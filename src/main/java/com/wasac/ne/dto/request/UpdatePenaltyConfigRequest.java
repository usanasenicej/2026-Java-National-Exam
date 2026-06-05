package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Update penalty configuration — only provided fields are changed.")
public class UpdatePenaltyConfigRequest {

    @Size(min = 1, max = 100)
    private String name;

    @DecimalMin(value = "0.0", message = "Percentage cannot be negative")
    private BigDecimal percentage;

    @Min(value = 0, message = "Grace period days cannot be negative")
    private Integer gracePeriodDays;

    private LocalDate effectiveTo;

    private Status status;
}

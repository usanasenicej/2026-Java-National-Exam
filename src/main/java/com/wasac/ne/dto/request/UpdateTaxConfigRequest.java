package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Update tax configuration — only provided fields are changed.")
public class UpdateTaxConfigRequest {

    @Size(min = 1, max = 100)
    private String name;

    @DecimalMin(value = "0.0", message = "Percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Percentage cannot exceed 100")
    private BigDecimal percentage;

    private LocalDate effectiveTo;

    private Status status;
}

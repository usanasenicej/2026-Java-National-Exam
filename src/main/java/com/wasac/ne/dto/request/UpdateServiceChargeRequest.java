package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Update service charge — only provided fields are changed.")
public class UpdateServiceChargeRequest {

    @Size(min = 1, max = 100)
    private String name;

    @DecimalMin(value = "0.0", message = "Amount cannot be negative")
    private BigDecimal amount;

    private LocalDate effectiveTo;

    private Status status;
}

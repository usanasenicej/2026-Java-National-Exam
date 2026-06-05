package com.wasac.ne.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Generate bill from meter reading")
public class GenerateBillRequest {

    @NotNull(message = "Meter reading ID is required")
    private Long meterReadingId;
}

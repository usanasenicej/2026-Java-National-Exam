package com.wasac.ne.dto.request;

import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.TariffType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Create tariff — ROLE_ADMIN. Includes consumption rates, service charge, VAT, and late penalty in one object.")
public class CreateTariffRequest {

    @NotBlank(message = "Tariff name is required")
    private String name;

    @NotNull(message = "Meter type is required")
    private MeterType meterType;

    @NotNull(message = "Tariff type is required (FLAT or TIERED)")
    private TariffType tariffType;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    @Schema(description = "Required when tariffType = FLAT. Price per consumed unit.")
    @DecimalMin(value = "0.0", message = "Flat rate cannot be negative")
    private BigDecimal flatRate;

    @Schema(description = "Fixed monthly service charge added to every bill. Default 0.")
    @DecimalMin(value = "0.0", message = "Service charge cannot be negative")
    private BigDecimal serviceChargeAmount;

    @Schema(description = "VAT percentage applied to (consumption + service charge). Range 0-100. Default 0.")
    @DecimalMin(value = "0.0", message = "VAT percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "VAT percentage cannot exceed 100")
    private BigDecimal vatPercentage;

    @Schema(description = "Late payment penalty percentage applied to outstanding balance when overdue. Default 0.")
    @DecimalMin(value = "0.0", message = "Late penalty percentage cannot be negative")
    private BigDecimal latePenaltyPercentage;

    @NotNull(message = "Status is required")
    private Status status;

    @Valid
    @Schema(description = "Required when tariffType = TIERED. Define consumption tiers.")
    private List<TariffTierRequest> tiers;
}

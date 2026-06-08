package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Update tariff — only provided fields are changed. " +
        "Note: changing rates creates a new version via the create endpoint. " +
        "Use this endpoint to update the name, status, or effectiveTo (deactivation date).")
public class UpdateTariffRequest {

    @Size(min = 1, max = 100)
    private String name;

    /** Update effectiveFrom — allowed on existing tariff versions without past-date restriction */
    private LocalDate effectiveFrom;

    /** Moving effectiveTo to a past date deactivates the tariff. */
    private LocalDate effectiveTo;

    /** Set to INACTIVE to retire this tariff version. */
    private Status status;

    /** Optional: update the flat rate directly (only valid for FLAT tariffs). */
    @DecimalMin(value = "0.01", message = "Flat rate must be greater than zero")
    private BigDecimal flatRate;

    /** Optional: update service charge amount. */
    @DecimalMin(value = "0.0", message = "Service charge cannot be negative")
    private BigDecimal serviceChargeAmount;

    /** Optional: update VAT percentage. Range 0-100. */
    @DecimalMin(value = "0.0", message = "VAT percentage cannot be negative")
    @jakarta.validation.constraints.DecimalMax(value = "100.0", message = "VAT percentage cannot exceed 100")
    private BigDecimal vatPercentage;

    /** Optional: update late penalty percentage. */
    @DecimalMin(value = "0.0", message = "Late penalty percentage cannot be negative")
    private BigDecimal latePenaltyPercentage;

    /** Optional: replace tier definitions (only valid for TIERED tariffs). */
    @Valid
    private List<TariffTierRequest> tiers;
}

package com.wasac.ne.dto.request;

import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.TariffType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Create tariff — ROLE_ADMIN. New version applies to future billing cycles only.")
public class CreateTariffRequest {

    @NotBlank
    private String name;

    @NotNull
    private MeterType meterType;

    @NotNull
    private TariffType tariffType;

    @NotNull
    private LocalDate effectiveFrom;

    @DecimalMin("0.0")
    private BigDecimal flatRate;

    @NotNull
    private Status status;

    @Valid
    private List<TariffTierRequest> tiers;
}

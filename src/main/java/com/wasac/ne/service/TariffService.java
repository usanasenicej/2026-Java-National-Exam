package com.wasac.ne.service;

import com.wasac.ne.dto.request.CreateTariffRequest;
import com.wasac.ne.dto.request.TariffTierRequest;
import com.wasac.ne.dto.request.UpdateTariffRequest;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.dto.response.TariffResponse;
import com.wasac.ne.entity.Tariff;
import com.wasac.ne.entity.TariffTier;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.TariffType;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRepository tariffRepository;
    private final AuditService auditService;

    @Transactional
    public TariffResponse create(CreateTariffRequest request) {
        validateTariffRequest(request);

        int nextVersion = tariffRepository.findTopByMeterTypeOrderByVersionDesc(request.getMeterType())
                .map(t -> t.getVersion() + 1)
                .orElse(1);

        tariffRepository.findTopByMeterTypeOrderByVersionDesc(request.getMeterType())
                .ifPresent(previous -> {
                    if (previous.getEffectiveTo() == null && previous.getStatus() == Status.ACTIVE) {
                        previous.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
                        tariffRepository.save(previous);
                    }
                });

        Tariff tariff = Tariff.builder()
                .name(request.getName())
                .meterType(request.getMeterType())
                .tariffType(request.getTariffType())
                .version(nextVersion)
                .effectiveFrom(request.getEffectiveFrom())
                .flatRate(request.getFlatRate())
                .serviceChargeAmount(request.getServiceChargeAmount() != null
                        ? request.getServiceChargeAmount() : BigDecimal.ZERO)
                .vatPercentage(request.getVatPercentage() != null
                        ? request.getVatPercentage() : BigDecimal.ZERO)
                .latePenaltyPercentage(request.getLatePenaltyPercentage() != null
                        ? request.getLatePenaltyPercentage() : BigDecimal.ZERO)
                .status(request.getStatus())
                .tiers(new ArrayList<>())
                .build();

        if (request.getTariffType() == TariffType.TIERED && request.getTiers() != null) {
            for (TariffTierRequest tierReq : request.getTiers()) {
                TariffTier tier = TariffTier.builder()
                        .tariff(tariff)
                        .tierOrder(tierReq.getTierOrder())
                        .minUnits(tierReq.getMinUnits())
                        .maxUnits(tierReq.getMaxUnits())
                        .ratePerUnit(tierReq.getRatePerUnit())
                        .build();
                tariff.getTiers().add(tier);
            }
        }

        tariff = tariffRepository.save(tariff);
        auditService.log("Tariff", tariff.getId(), "CREATE", "Tariff version " + nextVersion + " created");
        return EntityMapper.toTariffResponse(tariff);
    }

    @Transactional(readOnly = true)
    public TariffResponse getById(Long id) {
        return EntityMapper.toTariffResponse(findTariff(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TariffResponse> getAll(MeterType meterType, Pageable pageable) {
        Page<Tariff> page = meterType != null
                ? tariffRepository.findByMeterType(meterType, pageable)
                : tariffRepository.findAll(pageable);
        return EntityMapper.toPageResponse(page, EntityMapper::toTariffResponse);
    }

    @Transactional
    public void delete(Long id) {
        Tariff tariff = findTariff(id);
        auditService.log("Tariff", id, "DELETE",
                "name=" + tariff.getName() + ",type=" + tariff.getTariffType()
                        + ",version=" + tariff.getVersion() + ",meterType=" + tariff.getMeterType(),
                null,
                "Tariff deleted: " + tariff.getName() + " v" + tariff.getVersion());
        tariffRepository.delete(tariff);
    }

    @Transactional
    public TariffResponse update(Long id, UpdateTariffRequest request) {
        Tariff tariff = findTariff(id);

        String oldSnapshot = "name=" + tariff.getName() + ",status=" + tariff.getStatus()
                + ",effectiveFrom=" + tariff.getEffectiveFrom()
                + ",effectiveTo=" + tariff.getEffectiveTo() + ",flatRate=" + tariff.getFlatRate();

        if (request.getName() != null) tariff.setName(request.getName());
        if (request.getStatus() != null) tariff.setStatus(request.getStatus());
        if (request.getEffectiveTo() != null) tariff.setEffectiveTo(request.getEffectiveTo());
        // Allow updating effectiveFrom — no past-date restriction on updates, only on new versions
        if (request.getEffectiveFrom() != null) tariff.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getServiceChargeAmount() != null) {
            if (request.getServiceChargeAmount().compareTo(BigDecimal.ZERO) < 0)
                throw new BusinessException("Service charge amount cannot be negative");
            tariff.setServiceChargeAmount(request.getServiceChargeAmount());
        }
        if (request.getVatPercentage() != null) {
            if (request.getVatPercentage().compareTo(BigDecimal.ZERO) < 0
                    || request.getVatPercentage().compareTo(BigDecimal.valueOf(100)) > 0)
                throw new BusinessException("VAT percentage must be between 0 and 100");
            tariff.setVatPercentage(request.getVatPercentage());
        }
        if (request.getLatePenaltyPercentage() != null) {
            if (request.getLatePenaltyPercentage().compareTo(BigDecimal.ZERO) < 0)
                throw new BusinessException("Late penalty percentage cannot be negative");
            tariff.setLatePenaltyPercentage(request.getLatePenaltyPercentage());
        }

        // Update flat rate (FLAT tariffs only)
        if (request.getFlatRate() != null) {
            if (tariff.getTariffType() != TariffType.FLAT) {
                throw new BusinessException("Cannot set flatRate on a TIERED tariff");
            }
            if (request.getFlatRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Flat rate must be greater than zero");
            }
            tariff.setFlatRate(request.getFlatRate());
        }

        // Replace tier definitions (TIERED tariffs only)
        if (request.getTiers() != null && !request.getTiers().isEmpty()) {
            if (tariff.getTariffType() != TariffType.TIERED) {
                throw new BusinessException("Cannot set tiers on a FLAT tariff");
            }
            validateTierRanges(request.getTiers());
            tariff.getTiers().clear();
            for (TariffTierRequest tierReq : request.getTiers()) {
                TariffTier tier = TariffTier.builder()
                        .tariff(tariff)
                        .tierOrder(tierReq.getTierOrder())
                        .minUnits(tierReq.getMinUnits())
                        .maxUnits(tierReq.getMaxUnits())
                        .ratePerUnit(tierReq.getRatePerUnit())
                        .build();
                tariff.getTiers().add(tier);
            }
        }

        tariff = tariffRepository.save(tariff);

        String newSnapshot = "name=" + tariff.getName() + ",status=" + tariff.getStatus()
                + ",effectiveFrom=" + tariff.getEffectiveFrom()
                + ",effectiveTo=" + tariff.getEffectiveTo() + ",flatRate=" + tariff.getFlatRate();
        auditService.log("Tariff", tariff.getId(), "UPDATE", oldSnapshot, newSnapshot,
                "Tariff updated: " + tariff.getName() + " v" + tariff.getVersion());
        return EntityMapper.toTariffResponse(tariff);
    }

    public Tariff findActiveTariff(MeterType meterType, LocalDate billingDate) {
        return tariffRepository.findActiveTariffForDate(meterType, Status.ACTIVE, billingDate)
                .orElseThrow(() -> new BusinessException(
                        "No active tariff found for " + meterType + " on " + billingDate));
    }

    public BigDecimal calculateConsumptionCost(Tariff tariff, BigDecimal consumption) {
        if (tariff.getTariffType() == TariffType.FLAT) {
            return consumption.multiply(tariff.getFlatRate());
        }

        BigDecimal remaining = consumption;
        BigDecimal total = BigDecimal.ZERO;
        List<TariffTier> sortedTiers = tariff.getTiers().stream()
                .sorted((a, b) -> Integer.compare(a.getTierOrder(), b.getTierOrder()))
                .toList();

        for (TariffTier tier : sortedTiers) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal tierMax = tier.getMaxUnits() != null
                    ? tier.getMaxUnits().subtract(tier.getMinUnits())
                    : remaining;

            BigDecimal unitsInTier = remaining.min(tierMax);
            total = total.add(unitsInTier.multiply(tier.getRatePerUnit()));
            remaining = remaining.subtract(unitsInTier);
        }
        return total;
    }

    private Tariff findTariff(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found with id: " + id));
    }

    private void validateTariffRequest(CreateTariffRequest request) {
        // Validate effective date is not in the past
        if (request.getEffectiveFrom() != null && request.getEffectiveFrom().isBefore(LocalDate.now())) {
            throw new BusinessException("Tariff effective date cannot be in the past. Got: "
                    + request.getEffectiveFrom() + " (today is " + LocalDate.now() + ")");
        }

        if (request.getTariffType() == TariffType.FLAT) {
            if (request.getFlatRate() == null || request.getFlatRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Flat rate is required and must be greater than zero for FLAT tariffs");
            }
        } else {
            if (request.getTiers() == null || request.getTiers().isEmpty()) {
                throw new BusinessException("At least one tier is required for TIERED tariffs");
            }
            validateTierRanges(request.getTiers());
        }
    }

    /**
     * Validate that tier ranges do not overlap and cover consecutive units.
     * E.g. [0-10, 10-20] is invalid because 10 appears in both.
     * Valid example: [0-10, 11-30, 31-100, 101-null]
     */
    private void validateTierRanges(List<com.wasac.ne.dto.request.TariffTierRequest> tiers) {
        // Sort by tierOrder
        List<com.wasac.ne.dto.request.TariffTierRequest> sorted = tiers.stream()
                .sorted((a, b) -> Integer.compare(a.getTierOrder(), b.getTierOrder()))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            com.wasac.ne.dto.request.TariffTierRequest tier = sorted.get(i);

            // Each tier must have a rate > 0
            if (tier.getRatePerUnit() == null || tier.getRatePerUnit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Tier " + tier.getTierOrder()
                        + ": rate per unit must be greater than zero");
            }

            // Min units must be >= 0
            if (tier.getMinUnits() == null || tier.getMinUnits().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Tier " + tier.getTierOrder() + ": minUnits must be >= 0");
            }

            // maxUnits (if present) must be > minUnits
            if (tier.getMaxUnits() != null && tier.getMaxUnits().compareTo(tier.getMinUnits()) <= 0) {
                throw new BusinessException("Tier " + tier.getTierOrder()
                        + ": maxUnits (" + tier.getMaxUnits()
                        + ") must be greater than minUnits (" + tier.getMinUnits() + ")");
            }

            // Check overlap with next tier: this tier's maxUnits must be < next tier's minUnits
            if (i < sorted.size() - 1) {
                com.wasac.ne.dto.request.TariffTierRequest nextTier = sorted.get(i + 1);
                if (tier.getMaxUnits() == null) {
                    throw new BusinessException("Tier " + tier.getTierOrder()
                            + " has no maxUnits but is not the last tier. "
                            + "Only the final tier may have an open-ended (null) maxUnits.");
                }
                // e.g. tier maxUnits=10, next minUnits=10 → overlap at 10 (invalid)
                // Valid: tier maxUnits=10, next minUnits=11
                if (tier.getMaxUnits().compareTo(nextTier.getMinUnits()) >= 0) {
                    throw new BusinessException("Tier ranges overlap between tier "
                            + tier.getTierOrder() + " (max=" + tier.getMaxUnits()
                            + ") and tier " + nextTier.getTierOrder()
                            + " (min=" + nextTier.getMinUnits() + "). "
                            + "Ranges must not overlap (e.g. 0-10 then 11-30, not 0-10 then 10-30).");
                }
            }
        }
    }
}

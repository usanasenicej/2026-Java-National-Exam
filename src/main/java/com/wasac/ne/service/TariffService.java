package com.wasac.ne.service;

import com.wasac.ne.dto.request.CreateTariffRequest;
import com.wasac.ne.dto.request.TariffTierRequest;
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
        tariffRepository.delete(tariff);
        auditService.log("Tariff", id, "DELETE", "Tariff deleted");
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
        if (request.getTariffType() == TariffType.FLAT) {
            if (request.getFlatRate() == null || request.getFlatRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Flat rate is required and must be greater than zero for FLAT tariffs");
            }
        } else {
            if (request.getTiers() == null || request.getTiers().isEmpty()) {
                throw new BusinessException("At least one tier is required for TIERED tariffs");
            }
        }
    }
}

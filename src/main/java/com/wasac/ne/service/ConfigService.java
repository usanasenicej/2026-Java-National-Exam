package com.wasac.ne.service;

import com.wasac.ne.dto.request.CreatePenaltyConfigRequest;
import com.wasac.ne.dto.request.CreateServiceChargeRequest;
import com.wasac.ne.dto.request.CreateTaxConfigRequest;
import com.wasac.ne.dto.request.UpdatePenaltyConfigRequest;
import com.wasac.ne.dto.request.UpdateServiceChargeRequest;
import com.wasac.ne.dto.request.UpdateTaxConfigRequest;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.dto.response.PenaltyConfigResponse;
import com.wasac.ne.dto.response.ServiceChargeResponse;
import com.wasac.ne.dto.response.TaxConfigResponse;
import com.wasac.ne.entity.PenaltyConfig;
import com.wasac.ne.entity.ServiceCharge;
import com.wasac.ne.entity.TaxConfig;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.repository.PenaltyConfigRepository;
import com.wasac.ne.repository.ServiceChargeRepository;
import com.wasac.ne.repository.TaxConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ServiceChargeRepository serviceChargeRepository;
    private final TaxConfigRepository taxConfigRepository;
    private final PenaltyConfigRepository penaltyConfigRepository;
    private final AuditService auditService;

    @Transactional
    public ServiceChargeResponse createServiceCharge(CreateServiceChargeRequest request) {
        // Validation: amount must be >= 0
        if (request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new com.wasac.ne.exception.BusinessException("Service charge amount cannot be negative");
        }
        // Validation: effectiveFrom must not be in the past
        if (request.getEffectiveFrom() != null && request.getEffectiveFrom().isBefore(LocalDate.now())) {
            throw new com.wasac.ne.exception.BusinessException("Service charge effective date cannot be in the past");
        }

        int version = serviceChargeRepository.findAll().stream()
                .filter(s -> s.getMeterType() == request.getMeterType())
                .mapToInt(ServiceCharge::getVersion).max().orElse(0) + 1;

        ServiceCharge charge = ServiceCharge.builder()
                .name(request.getName())
                .meterType(request.getMeterType())
                .amount(request.getAmount())
                .version(version)
                .effectiveFrom(request.getEffectiveFrom())
                .status(request.getStatus())
                .build();
        charge = serviceChargeRepository.save(charge);
        auditService.log("ServiceCharge", charge.getId(), "CREATE", "Service charge created");
        return toServiceChargeResponse(charge);
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceChargeResponse> getServiceCharges(MeterType meterType, Pageable pageable) {
        Page<ServiceCharge> page = meterType != null
                ? serviceChargeRepository.findByMeterType(meterType, pageable)
                : serviceChargeRepository.findAll(pageable);
        return mapPage(page, this::toServiceChargeResponse);
    }

    @Transactional
    public void deleteServiceCharge(Long id) {
        serviceChargeRepository.deleteById(id);
        auditService.log("ServiceCharge", id, "DELETE", "Service charge deleted");
    }

    @Transactional
    public TaxConfigResponse createTax(CreateTaxConfigRequest request) {
        // Validation: percentage must be between 0 and 100
        if (request.getPercentage().compareTo(BigDecimal.ZERO) < 0
                || request.getPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new com.wasac.ne.exception.BusinessException("Tax percentage must be between 0 and 100");
        }
        // Validation: effectiveFrom must not be in the past
        if (request.getEffectiveFrom() != null && request.getEffectiveFrom().isBefore(LocalDate.now())) {
            throw new com.wasac.ne.exception.BusinessException("Tax effective date cannot be in the past");
        }

        int version = taxConfigRepository.findAll().stream()
                .mapToInt(TaxConfig::getVersion).max().orElse(0) + 1;

        TaxConfig tax = TaxConfig.builder()
                .name(request.getName())
                .percentage(request.getPercentage())
                .version(version)
                .effectiveFrom(request.getEffectiveFrom())
                .status(request.getStatus())
                .build();
        tax = taxConfigRepository.save(tax);
        auditService.log("TaxConfig", tax.getId(), "CREATE", "Tax config created");
        return toTaxResponse(tax);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaxConfigResponse> getTaxes(Pageable pageable) {
        return mapPage(taxConfigRepository.findAll(pageable), this::toTaxResponse);
    }

    @Transactional
    public void deleteTax(Long id) {
        taxConfigRepository.deleteById(id);
        auditService.log("TaxConfig", id, "DELETE", "Tax config deleted");
    }

    @Transactional
    public PenaltyConfigResponse createPenalty(CreatePenaltyConfigRequest request) {
        // Validation: percentage must be >= 0
        if (request.getPercentage().compareTo(BigDecimal.ZERO) < 0) {
            throw new com.wasac.ne.exception.BusinessException("Penalty percentage cannot be negative");
        }
        // Validation: grace period must be >= 0
        if (request.getGracePeriodDays() < 0) {
            throw new com.wasac.ne.exception.BusinessException("Grace period days cannot be negative");
        }
        // Validation: effectiveFrom must not be in the past
        if (request.getEffectiveFrom() != null && request.getEffectiveFrom().isBefore(LocalDate.now())) {
            throw new com.wasac.ne.exception.BusinessException("Penalty effective date cannot be in the past");
        }

        int version = penaltyConfigRepository.findAll().stream()
                .mapToInt(PenaltyConfig::getVersion).max().orElse(0) + 1;

        PenaltyConfig penalty = PenaltyConfig.builder()
                .name(request.getName())
                .percentage(request.getPercentage())
                .gracePeriodDays(request.getGracePeriodDays())
                .version(version)
                .effectiveFrom(request.getEffectiveFrom())
                .status(request.getStatus())
                .build();
        penalty = penaltyConfigRepository.save(penalty);
        auditService.log("PenaltyConfig", penalty.getId(), "CREATE", "Penalty config created");
        return toPenaltyResponse(penalty);
    }

    @Transactional(readOnly = true)
    public PageResponse<PenaltyConfigResponse> getPenalties(Pageable pageable) {
        return mapPage(penaltyConfigRepository.findAll(pageable), this::toPenaltyResponse);
    }

    @Transactional
    public void deletePenalty(Long id) {
        penaltyConfigRepository.deleteById(id);
        auditService.log("PenaltyConfig", id, "DELETE", "Penalty config deleted");
    }

    // ─── Update methods ───────────────────────────────────────────────────────

    @Transactional
    public ServiceChargeResponse updateServiceCharge(Long id, UpdateServiceChargeRequest request) {
        ServiceCharge charge = serviceChargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service charge not found with id: " + id));

        String oldSnapshot = "name=" + charge.getName() + ",amount=" + charge.getAmount()
                + ",status=" + charge.getStatus();

        if (request.getName() != null) charge.setName(request.getName());
        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new com.wasac.ne.exception.BusinessException("Service charge amount cannot be negative");
            }
            charge.setAmount(request.getAmount());
        }
        if (request.getEffectiveTo() != null) charge.setEffectiveTo(request.getEffectiveTo());
        if (request.getStatus() != null) charge.setStatus(request.getStatus());

        charge = serviceChargeRepository.save(charge);
        String newSnapshot = "name=" + charge.getName() + ",amount=" + charge.getAmount()
                + ",status=" + charge.getStatus();
        auditService.log("ServiceCharge", charge.getId(), "UPDATE", oldSnapshot, newSnapshot,
                "Service charge updated: " + charge.getName());
        return toServiceChargeResponse(charge);
    }

    @Transactional
    public TaxConfigResponse updateTax(Long id, UpdateTaxConfigRequest request) {
        TaxConfig tax = taxConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax config not found with id: " + id));

        String oldSnapshot = "name=" + tax.getName() + ",percentage=" + tax.getPercentage()
                + ",status=" + tax.getStatus();

        if (request.getName() != null) tax.setName(request.getName());
        if (request.getPercentage() != null) {
            if (request.getPercentage().compareTo(BigDecimal.ZERO) < 0
                    || request.getPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new com.wasac.ne.exception.BusinessException("Tax percentage must be between 0 and 100");
            }
            tax.setPercentage(request.getPercentage());
        }
        if (request.getEffectiveTo() != null) tax.setEffectiveTo(request.getEffectiveTo());
        if (request.getStatus() != null) tax.setStatus(request.getStatus());

        tax = taxConfigRepository.save(tax);
        String newSnapshot = "name=" + tax.getName() + ",percentage=" + tax.getPercentage()
                + ",status=" + tax.getStatus();
        auditService.log("TaxConfig", tax.getId(), "UPDATE", oldSnapshot, newSnapshot,
                "Tax config updated: " + tax.getName());
        return toTaxResponse(tax);
    }

    @Transactional
    public PenaltyConfigResponse updatePenalty(Long id, UpdatePenaltyConfigRequest request) {
        PenaltyConfig penalty = penaltyConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty config not found with id: " + id));

        String oldSnapshot = "name=" + penalty.getName() + ",percentage=" + penalty.getPercentage()
                + ",gracePeriodDays=" + penalty.getGracePeriodDays() + ",status=" + penalty.getStatus();

        if (request.getName() != null) penalty.setName(request.getName());
        if (request.getPercentage() != null) {
            if (request.getPercentage().compareTo(BigDecimal.ZERO) < 0) {
                throw new com.wasac.ne.exception.BusinessException("Penalty percentage cannot be negative");
            }
            penalty.setPercentage(request.getPercentage());
        }
        if (request.getGracePeriodDays() != null) {
            if (request.getGracePeriodDays() < 0) {
                throw new com.wasac.ne.exception.BusinessException("Grace period days cannot be negative");
            }
            penalty.setGracePeriodDays(request.getGracePeriodDays());
        }
        if (request.getEffectiveTo() != null) penalty.setEffectiveTo(request.getEffectiveTo());
        if (request.getStatus() != null) penalty.setStatus(request.getStatus());

        penalty = penaltyConfigRepository.save(penalty);
        String newSnapshot = "name=" + penalty.getName() + ",percentage=" + penalty.getPercentage()
                + ",gracePeriodDays=" + penalty.getGracePeriodDays() + ",status=" + penalty.getStatus();
        auditService.log("PenaltyConfig", penalty.getId(), "UPDATE", oldSnapshot, newSnapshot,
                "Penalty config updated: " + penalty.getName());
        return toPenaltyResponse(penalty);
    }

    public ServiceCharge findActiveServiceCharge(MeterType meterType, LocalDate date) {
        return serviceChargeRepository.findActiveForDate(meterType, Status.ACTIVE, date)
                .orElseThrow(() -> new com.wasac.ne.exception.BusinessException(
                        "No active service charge for " + meterType));
    }

    public TaxConfig findActiveTax(LocalDate date) {
        return taxConfigRepository.findActiveForDate(Status.ACTIVE, date)
                .orElseThrow(() -> new com.wasac.ne.exception.BusinessException("No active tax configuration"));
    }

    public PenaltyConfig findActivePenalty(LocalDate date) {
        return penaltyConfigRepository.findActiveForDate(Status.ACTIVE, date)
                .orElseThrow(() -> new com.wasac.ne.exception.BusinessException("No active penalty configuration"));
    }

    private ServiceChargeResponse toServiceChargeResponse(ServiceCharge s) {
        return ServiceChargeResponse.builder()
                .id(s.getId()).name(s.getName()).meterType(s.getMeterType())
                .amount(s.getAmount()).version(s.getVersion())
                .effectiveFrom(s.getEffectiveFrom()).effectiveTo(s.getEffectiveTo())
                .status(s.getStatus()).createdAt(s.getCreatedAt()).build();
    }

    private TaxConfigResponse toTaxResponse(TaxConfig t) {
        return TaxConfigResponse.builder()
                .id(t.getId()).name(t.getName()).percentage(t.getPercentage())
                .version(t.getVersion()).effectiveFrom(t.getEffectiveFrom())
                .effectiveTo(t.getEffectiveTo()).status(t.getStatus())
                .createdAt(t.getCreatedAt()).build();
    }

    private PenaltyConfigResponse toPenaltyResponse(PenaltyConfig p) {
        return PenaltyConfigResponse.builder()
                .id(p.getId()).name(p.getName()).percentage(p.getPercentage())
                .gracePeriodDays(p.getGracePeriodDays()).version(p.getVersion())
                .effectiveFrom(p.getEffectiveFrom()).effectiveTo(p.getEffectiveTo())
                .status(p.getStatus()).createdAt(p.getCreatedAt()).build();
    }

    private <T, R> PageResponse<R> mapPage(Page<T> page, java.util.function.Function<T, R> mapper) {
        return com.wasac.ne.mapper.EntityMapper.toPageResponse(page, mapper);
    }
}

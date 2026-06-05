package com.wasac.ne.service;

import com.wasac.ne.dto.request.CreatePenaltyConfigRequest;
import com.wasac.ne.dto.request.CreateServiceChargeRequest;
import com.wasac.ne.dto.request.CreateTaxConfigRequest;
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

    public ServiceCharge findActiveServiceCharge(MeterType meterType, LocalDate date) {
        return serviceChargeRepository.findActiveForDate(meterType, Status.ACTIVE, date)
                .orElseThrow(() -> new com.wasac.ne.exception.BusinessException(
                        "No active service charge for " + meterType));
    }

    public TaxConfig findActiveTax(LocalDate date) {
        return taxConfigRepository.findActiveForDate(Status.ACTIVE, date)
                .orElseThrow(() -> new com.wasac.ne.exception.BusinessException("No active tax configuration"));
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

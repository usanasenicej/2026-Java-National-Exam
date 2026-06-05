package com.wasac.ne.service;

import com.wasac.ne.dto.request.CreateMeterRequest;
import com.wasac.ne.dto.request.UpdateMeterRequest;
import com.wasac.ne.dto.response.MeterResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.entity.Customer;
import com.wasac.ne.entity.Meter;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterRepository meterRepository;
    private final CustomerService customerService;
    private final AuditService auditService;

    @Transactional
    public MeterResponse create(CreateMeterRequest request) {
        if (meterRepository.existsByMeterNumber(request.getMeterNumber())) {
            throw new BusinessException("Meter number '" + request.getMeterNumber() + "' already exists");
        }

        Customer customer = customerService.findCustomer(request.getCustomerId());

        Meter meter = Meter.builder()
                .meterNumber(request.getMeterNumber().trim())
                .meterType(request.getMeterType())
                .installationDate(request.getInstallationDate())
                .status(request.getStatus())
                .customer(customer)
                .build();

        meter = meterRepository.save(meter);
        auditService.log("Meter", meter.getId(), "CREATE", "Meter installed for customer " + customer.getId());
        return EntityMapper.toMeterResponse(meter);
    }

    @Transactional(readOnly = true)
    public MeterResponse getById(Long id) {
        return EntityMapper.toMeterResponse(findMeter(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MeterResponse> getAll(String search, Long customerId, MeterType type, Pageable pageable) {
        Page<Meter> page;
        if (customerId != null) {
            page = meterRepository.findByCustomerId(customerId, pageable);
        } else if (search != null && !search.isBlank()) {
            page = meterRepository.findByMeterNumberContainingIgnoreCase(search, pageable);
        } else if (type != null) {
            page = meterRepository.findByMeterTypeAndStatus(type, Status.ACTIVE, pageable);
        } else {
            page = meterRepository.findAll(pageable);
        }
        return EntityMapper.toPageResponse(page, EntityMapper::toMeterResponse);
    }

    @Transactional
    public MeterResponse update(Long id, UpdateMeterRequest request) {
        Meter meter = findMeter(id);
        if (request.getMeterType() != null) meter.setMeterType(request.getMeterType());
        if (request.getInstallationDate() != null) meter.setInstallationDate(request.getInstallationDate());
        if (request.getStatus() != null) meter.setStatus(request.getStatus());
        meter = meterRepository.save(meter);
        auditService.log("Meter", meter.getId(), "UPDATE", "Meter updated");
        return EntityMapper.toMeterResponse(meter);
    }

    @Transactional
    public void delete(Long id) {
        Meter meter = findMeter(id);
        meterRepository.delete(meter);
        auditService.log("Meter", id, "DELETE", "Meter deleted");
    }

    public Meter findMeter(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + id));
    }
}

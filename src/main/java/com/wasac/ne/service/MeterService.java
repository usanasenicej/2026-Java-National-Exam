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
    private final OwnershipService ownershipService;

    @Transactional
    public MeterResponse create(CreateMeterRequest request) {
        if (meterRepository.existsByMeterNumber(request.getMeterNumber())) {
            throw new BusinessException("Meter number '" + request.getMeterNumber() + "' already exists");
        }

        Customer customer = customerService.findCustomer(request.getCustomerId());

        // Validation: meter must only be assigned to active customers
        if (customer.getStatus() != com.wasac.ne.enums.Status.ACTIVE) {
            throw new BusinessException("Cannot assign a meter to an inactive/suspended customer: "
                    + customer.getFullNames());
        }

        Meter meter = Meter.builder()
                .meterNumber(request.getMeterNumber().trim())
                .meterType(request.getMeterType())
                .installationDate(request.getInstallationDate())
                .status(request.getStatus())
                .customer(customer)
                .build();

        meter = meterRepository.save(meter);
        auditService.log("Meter", meter.getId(), "CREATE",
                null,
                "meterNumber=" + meter.getMeterNumber() + ",type=" + meter.getMeterType()
                        + ",customerId=" + customer.getId(),
                "Meter installed for customer " + customer.getFullNames());
        return EntityMapper.toMeterResponse(meter);
    }

    @Transactional(readOnly = true)
    public MeterResponse getById(Long id) {
        Meter meter = findMeter(id);
        ownershipService.assertOwnership(meter.getCustomer().getId(), "Meter");
        return EntityMapper.toMeterResponse(meter);
    }

    @Transactional(readOnly = true)
    public PageResponse<MeterResponse> getAll(String search, Long customerId, MeterType type, Pageable pageable) {
        // ROLE_CUSTOMER can only see their own meters
        Long ownedCustomerId = ownershipService.getOwnedCustomerIdOrNull();
        if (ownedCustomerId != null) {
            customerId = ownedCustomerId;
        }

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

        String oldSnapshot = "meterType=" + meter.getMeterType()
                + ",status=" + meter.getStatus()
                + ",installationDate=" + meter.getInstallationDate();

        if (request.getMeterType() != null) meter.setMeterType(request.getMeterType());
        if (request.getInstallationDate() != null) meter.setInstallationDate(request.getInstallationDate());
        if (request.getStatus() != null) meter.setStatus(request.getStatus());
        meter = meterRepository.save(meter);

        String newSnapshot = "meterType=" + meter.getMeterType()
                + ",status=" + meter.getStatus()
                + ",installationDate=" + meter.getInstallationDate();

        auditService.log("Meter", meter.getId(), "UPDATE",
                oldSnapshot, newSnapshot, "Meter updated: " + meter.getMeterNumber());
        return EntityMapper.toMeterResponse(meter);
    }

    @Transactional
    public void delete(Long id) {
        Meter meter = findMeter(id);
        auditService.log("Meter", id, "DELETE",
                "meterNumber=" + meter.getMeterNumber() + ",status=" + meter.getStatus(), null,
                "Meter deleted: " + meter.getMeterNumber());
        meterRepository.delete(meter);
    }

    public Meter findMeter(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + id));
    }
}

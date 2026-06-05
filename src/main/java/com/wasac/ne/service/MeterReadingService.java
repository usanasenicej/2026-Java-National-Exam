package com.wasac.ne.service;

import com.wasac.ne.dto.request.CreateMeterReadingRequest;
import com.wasac.ne.dto.response.MeterReadingResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.entity.Meter;
import com.wasac.ne.entity.MeterReading;
import com.wasac.ne.enums.Status;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterService meterService;
    private final AuditService auditService;

    @Transactional
    public MeterReadingResponse create(CreateMeterReadingRequest request) {
        Meter meter = meterService.findMeter(request.getMeterId());

        // 1. Meter must be active — not inactive, suspended, or disconnected
        if (meter.getStatus() != Status.ACTIVE) {
            throw new BusinessException("Cannot capture reading for meter '" + meter.getMeterNumber()
                    + "': meter status is " + meter.getStatus() + " (must be ACTIVE)");
        }

        // 2. Current reading must be strictly greater than previous reading
        if (request.getCurrentReading().compareTo(request.getPreviousReading()) <= 0) {
            throw new BusinessException("Current reading (" + request.getCurrentReading() +
                    ") must be greater than previous reading (" + request.getPreviousReading() + ")");
        }

        // 3. Consumption (computed) must not be negative
        BigDecimal consumption = request.getCurrentReading().subtract(request.getPreviousReading());
        if (consumption.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Consumption cannot be negative. Got: " + consumption);
        }

        // 4. Reading date must not be in the future (already covered by @PastOrPresent in DTO,
        //    but we add a service-level guard for programmatic callers)
        if (request.getReadingDate().isAfter(java.time.LocalDate.now())) {
            throw new BusinessException("Reading date cannot be in the future: " + request.getReadingDate());
        }

        int month = request.getReadingDate().getMonthValue();
        int year  = request.getReadingDate().getYear();

        // 5. Enforce one reading per meter per month/year
        if (meterReadingRepository.existsByMeterIdAndReadingMonthAndReadingYear(meter.getId(), month, year)) {
            throw new BusinessException("A reading for meter '" + meter.getMeterNumber() +
                    "' already exists for " + month + "/" + year + ". Only one reading per meter per month is allowed.");
        }

        MeterReading reading = MeterReading.builder()
                .meter(meter)
                .previousReading(request.getPreviousReading())
                .currentReading(request.getCurrentReading())
                .readingDate(request.getReadingDate())
                .readingMonth(month)
                .readingYear(year)
                .consumption(consumption)
                .build();

        reading = meterReadingRepository.save(reading);
        auditService.log("MeterReading", reading.getId(), "CREATE",
                null,
                "meterId=" + meter.getId() + ",meter=" + meter.getMeterNumber()
                        + ",consumption=" + consumption + ",period=" + month + "/" + year,
                "Reading captured for meter " + meter.getMeterNumber()
                        + " (" + month + "/" + year + "): " + consumption + " units");
        return EntityMapper.toMeterReadingResponse(reading);
    }

    @Transactional(readOnly = true)
    public MeterReadingResponse getById(Long id) {
        return EntityMapper.toMeterReadingResponse(findReading(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MeterReadingResponse> getAll(Long meterId, Integer month, Integer year, Pageable pageable) {
        Page<MeterReading> page;
        if (meterId != null) {
            page = meterReadingRepository.findByMeterId(meterId, pageable);
        } else if (month != null && year != null) {
            page = meterReadingRepository.findByReadingMonthAndReadingYear(month, year, pageable);
        } else {
            page = meterReadingRepository.findAll(pageable);
        }
        return EntityMapper.toPageResponse(page, EntityMapper::toMeterReadingResponse);
    }

    @Transactional
    public void delete(Long id) {
        MeterReading reading = findReading(id);
        auditService.log("MeterReading", id, "DELETE",
                "meterId=" + reading.getMeter().getId() + ",period="
                        + reading.getReadingMonth() + "/" + reading.getReadingYear(), null,
                "Meter reading deleted for meter " + reading.getMeter().getMeterNumber());
        meterReadingRepository.delete(reading);
    }

    public MeterReading findReading(Long id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with id: " + id));
    }
}

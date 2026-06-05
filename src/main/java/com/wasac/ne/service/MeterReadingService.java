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

        if (meter.getStatus() != Status.ACTIVE) {
            throw new BusinessException("Cannot capture reading for inactive meter: " + meter.getMeterNumber());
        }

        if (request.getCurrentReading().compareTo(request.getPreviousReading()) <= 0) {
            throw new BusinessException("Current reading (" + request.getCurrentReading() +
                    ") must be greater than previous reading (" + request.getPreviousReading() + ")");
        }

        int month = request.getReadingDate().getMonthValue();
        int year = request.getReadingDate().getYear();

        if (meterReadingRepository.existsByMeterIdAndReadingMonthAndReadingYear(meter.getId(), month, year)) {
            throw new BusinessException("A reading for meter '" + meter.getMeterNumber() +
                    "' already exists for " + month + "/" + year);
        }

        BigDecimal consumption = request.getCurrentReading().subtract(request.getPreviousReading());

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
                "Reading captured for meter " + meter.getMeterNumber());
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
        meterReadingRepository.delete(reading);
        auditService.log("MeterReading", id, "DELETE", "Meter reading deleted");
    }

    public MeterReading findReading(Long id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with id: " + id));
    }
}

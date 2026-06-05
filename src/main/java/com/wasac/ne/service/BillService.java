package com.wasac.ne.service;

import com.wasac.ne.dto.request.GenerateBillRequest;
import com.wasac.ne.dto.response.BillResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.entity.*;
import com.wasac.ne.enums.BillStatus;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.BillRepository;
import com.wasac.ne.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final MeterReadingService meterReadingService;
    private final CustomerService customerService;
    private final TariffService tariffService;
    private final ConfigService configService;
    private final AuditService auditService;

    @Transactional
    public BillResponse generateBill(GenerateBillRequest request) {
        MeterReading reading = meterReadingService.findReading(request.getMeterReadingId());
        Meter meter = reading.getMeter();
        Customer customer = meter.getCustomer();

        customerService.ensureCustomerActive(customer);

        if (billRepository.findAll().stream().anyMatch(b ->
                b.getMeterReading() != null && b.getMeterReading().getId().equals(reading.getId()))) {
            throw new BusinessException("A bill already exists for this meter reading");
        }

        LocalDate billingDate = reading.getReadingDate();
        MeterType meterType = meter.getMeterType();

        Tariff tariff = tariffService.findActiveTariff(meterType, billingDate);
        ServiceCharge serviceCharge = configService.findActiveServiceCharge(meterType, billingDate);
        TaxConfig tax = configService.findActiveTax(billingDate);

        BigDecimal consumptionAmount = tariffService.calculateConsumptionCost(tariff, reading.getConsumption())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceChargeAmount = serviceCharge.getAmount();
        BigDecimal subtotal = consumptionAmount.add(serviceChargeAmount);
        BigDecimal taxAmount = subtotal.multiply(tax.getPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        String billRef = "BILL-" + reading.getReadingYear() +
                String.format("%02d", reading.getReadingMonth()) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Bill bill = Bill.builder()
                .billReference(billRef)
                .customer(customer)
                .meterReading(reading)
                .billingMonth(reading.getReadingMonth())
                .billingYear(reading.getReadingYear())
                .consumptionAmount(consumptionAmount)
                .serviceChargeAmount(serviceChargeAmount)
                .taxAmount(taxAmount)
                .penaltyAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .amountPaid(BigDecimal.ZERO)
                .outstandingBalance(totalAmount)
                .status(BillStatus.PENDING)
                .dueDate(billingDate.plusDays(30))
                .build();

        bill = billRepository.save(bill);
        auditService.log("Bill", bill.getId(), "GENERATE", "Bill generated: " + billRef);
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional
    public BillResponse approveBill(Long id) {
        Bill bill = findBill(id);
        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BusinessException("Only PENDING bills can be approved. Current status: " + bill.getStatus());
        }
        bill.setStatus(BillStatus.APPROVED);
        bill.setApprovedAt(LocalDate.now());
        bill.setApprovedBy(SecurityUtils.getCurrentUserEmail());
        bill = billRepository.save(bill);
        auditService.log("Bill", bill.getId(), "APPROVE", "Bill approved by " + bill.getApprovedBy());
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional
    public BillResponse rejectBill(Long id) {
        Bill bill = findBill(id);
        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BusinessException("Only PENDING bills can be rejected");
        }
        bill.setStatus(BillStatus.REJECTED);
        bill = billRepository.save(bill);
        auditService.log("Bill", bill.getId(), "REJECT", "Bill rejected");
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional(readOnly = true)
    public BillResponse getById(Long id) {
        return EntityMapper.toBillResponse(findBill(id));
    }

    @Transactional(readOnly = true)
    public BillResponse getByReference(String reference) {
        return EntityMapper.toBillResponse(billRepository.findByBillReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + reference)));
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> getAll(Long customerId, BillStatus status, String search, Pageable pageable) {
        Page<Bill> page;
        if (customerId != null) {
            page = billRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            page = billRepository.findByStatus(status, pageable);
        } else if (search != null && !search.isBlank()) {
            page = billRepository.findByBillReferenceContainingIgnoreCase(search, pageable);
        } else {
            page = billRepository.findAll(pageable);
        }
        return EntityMapper.toPageResponse(page, EntityMapper::toBillResponse);
    }

    @Transactional
    public void delete(Long id) {
        Bill bill = findBill(id);
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessException("Cannot delete a paid bill");
        }
        billRepository.delete(bill);
        auditService.log("Bill", id, "DELETE", "Bill deleted");
    }

    public Bill findBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }
}

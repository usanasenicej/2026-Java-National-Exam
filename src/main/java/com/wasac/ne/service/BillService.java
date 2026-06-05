package com.wasac.ne.service;

import com.wasac.ne.dto.request.GenerateBillRequest;
import com.wasac.ne.dto.response.BillResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.entity.*;
import com.wasac.ne.enums.BillStatus;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.NotificationType;
import com.wasac.ne.enums.Status;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.BillRepository;
import com.wasac.ne.repository.MeterRepository;
import com.wasac.ne.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    // Meter disconnection grace: bills overdue for more than this many days trigger disconnection
    private static final int DISCONNECTION_THRESHOLD_DAYS = 90;

    private final BillRepository billRepository;
    private final MeterReadingService meterReadingService;
    private final CustomerService customerService;
    private final TariffService tariffService;
    private final ConfigService configService;
    private final NotificationService notificationService;
    private final MeterRepository meterRepository;
    private final AuditService auditService;
    private final OwnershipService ownershipService;

    // ─────────────────────────────────────────────────────────────────────
    // Bill Generation
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public BillResponse generateBill(GenerateBillRequest request) {
        MeterReading reading = meterReadingService.findReading(request.getMeterReadingId());
        Meter meter = reading.getMeter();
        Customer customer = meter.getCustomer();

        // 1. Customer must be active
        customerService.ensureCustomerActive(customer);

        // 2. No duplicate bill for this exact meter reading
        if (billRepository.existsByMeterReadingId(reading.getId())) {
            throw new BusinessException("A bill already exists for this meter reading (id=" + reading.getId() + ")");
        }

        // 3. No duplicate bill for same meter + billing month/year
        int billingMonth = reading.getReadingMonth();
        int billingYear  = reading.getReadingYear();
        if (billRepository.existsByMeterIdAndBillingMonthAndBillingYear(meter.getId(), billingMonth, billingYear)) {
            throw new BusinessException("A bill already exists for meter '" + meter.getMeterNumber()
                    + "' for " + billingMonth + "/" + billingYear);
        }

        // 4. Consumption must be > 0
        if (reading.getConsumption().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Cannot generate bill: consumption must be greater than zero. Got: "
                    + reading.getConsumption());
        }

        LocalDate billingDate = reading.getReadingDate();
        MeterType meterType  = meter.getMeterType();

        // 5. Resolve active configurations
        Tariff tariff             = tariffService.findActiveTariff(meterType, billingDate);
        ServiceCharge serviceCharge = configService.findActiveServiceCharge(meterType, billingDate);
        TaxConfig tax             = configService.findActiveTax(billingDate);

        // 6. Calculate amounts
        BigDecimal consumptionAmount = tariffService
                .calculateConsumptionCost(tariff, reading.getConsumption())
                .setScale(2, RoundingMode.HALF_UP);

        if (consumptionAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Calculated consumption amount must not be negative");
        }

        BigDecimal serviceChargeAmount = serviceCharge.getAmount();
        BigDecimal subtotal            = consumptionAmount.add(serviceChargeAmount);
        BigDecimal taxAmount           = subtotal
                .multiply(tax.getPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount         = subtotal.add(taxAmount);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Calculated total bill amount must not be negative");
        }

        // 7. Build bill reference
        String billRef = "BILL-" + billingYear
                + String.format("%02d", billingMonth)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Bill bill = Bill.builder()
                .billReference(billRef)
                .customer(customer)
                .meterReading(reading)
                .billingMonth(billingMonth)
                .billingYear(billingYear)
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

        // 8. Create in-app notification for customer
        String notificationMessage = String.format(
                "Dear %s, your utility bill for %s %d has been generated. " +
                "Bill reference: %s. Total amount: %s FRW. Due date: %s.",
                customer.getFullNames(),
                getMonthName(billingMonth), billingYear,
                billRef, totalAmount, bill.getDueDate());

        notificationService.createNotification(customer, notificationMessage,
                NotificationType.BILL_GENERATED, bill.getId());

        auditService.log("Bill", bill.getId(), "GENERATE",
                null, "ref=" + billRef + ",total=" + totalAmount,
                "Bill generated for meter " + meter.getMeterNumber()
                        + " (" + billingMonth + "/" + billingYear + ")");

        return EntityMapper.toBillResponse(bill);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Approve / Reject
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public BillResponse approveBill(Long id) {
        Bill bill = findBill(id);
        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BusinessException("Only PENDING bills can be approved. Current status: " + bill.getStatus());
        }
        String oldStatus = bill.getStatus().name();
        bill.setStatus(BillStatus.APPROVED);
        bill.setApprovedAt(LocalDate.now());
        bill.setApprovedBy(SecurityUtils.getCurrentUserEmail());
        bill = billRepository.save(bill);

        auditService.log("Bill", bill.getId(), "APPROVE",
                "status=" + oldStatus, "status=APPROVED,approvedBy=" + bill.getApprovedBy(),
                "Bill approved: " + bill.getBillReference());
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional
    public BillResponse rejectBill(Long id) {
        Bill bill = findBill(id);
        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BusinessException("Only PENDING bills can be rejected. Current status: " + bill.getStatus());
        }
        String oldStatus = bill.getStatus().name();
        bill.setStatus(BillStatus.REJECTED);
        bill = billRepository.save(bill);

        auditService.log("Bill", bill.getId(), "REJECT",
                "status=" + oldStatus, "status=REJECTED",
                "Bill rejected: " + bill.getBillReference());
        return EntityMapper.toBillResponse(bill);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Overdue Processing — called by scheduler or manually via endpoint
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Process overdue bills:
     * 1. Find all APPROVED bills whose due date has passed → mark OVERDUE + apply penalty.
     * 2. Find all OVERDUE bills past the disconnection threshold → disconnect the meter.
     *
     * @return number of bills processed
     */
    @Transactional
    public int processOverdueBills() {
        LocalDate today = LocalDate.now();
        int processed = 0;

        // ── Step 1: APPROVED → OVERDUE + penalty ──────────────────────────
        List<Bill> approvedOverdue = billRepository.findOverdueBills(BillStatus.APPROVED, today);
        for (Bill bill : approvedOverdue) {
            try {
                PenaltyConfig penalty = configService.findActivePenalty(today);
                BigDecimal penaltyAmount = bill.getOutstandingBalance()
                        .multiply(penalty.getPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                String oldSnapshot = "status=APPROVED,outstanding=" + bill.getOutstandingBalance();
                bill.setPenaltyAmount(bill.getPenaltyAmount().add(penaltyAmount));
                bill.setTotalAmount(bill.getTotalAmount().add(penaltyAmount));
                bill.setOutstandingBalance(bill.getOutstandingBalance().add(penaltyAmount));
                bill.setStatus(BillStatus.OVERDUE);
                billRepository.save(bill);

                // Notify customer
                String msg = String.format(
                        "Dear %s, your bill %s is overdue. A late payment penalty of %s FRW has been applied. " +
                        "New outstanding balance: %s FRW. Please pay immediately to avoid disconnection.",
                        bill.getCustomer().getFullNames(), bill.getBillReference(),
                        penaltyAmount, bill.getOutstandingBalance());
                notificationService.createNotification(bill.getCustomer(), msg,
                        NotificationType.PENALTY_APPLIED, bill.getId());

                auditService.log("Bill", bill.getId(), "OVERDUE",
                        oldSnapshot,
                        "status=OVERDUE,penalty=" + penaltyAmount + ",outstanding=" + bill.getOutstandingBalance(),
                        "Overdue penalty applied: " + bill.getBillReference());

                processed++;
            } catch (Exception e) {
                log.error("Failed to process overdue bill id={}: {}", bill.getId(), e.getMessage());
            }
        }

        // ── Step 2: OVERDUE past disconnection threshold → disconnect meter ──
        LocalDate disconnectionCutoff = today.minusDays(DISCONNECTION_THRESHOLD_DAYS);
        List<Bill> longOverdue = billRepository.findBillsOverdueSince(disconnectionCutoff);
        for (Bill bill : longOverdue) {
            if (bill.getMeterReading() == null) continue;
            Meter meter = bill.getMeterReading().getMeter();
            if (meter.getStatus() == Status.DISCONNECTED) continue; // already disconnected

            String oldMeterStatus = meter.getStatus().name();
            meter.setStatus(Status.DISCONNECTED);
            meterRepository.save(meter);

            String msg = String.format(
                    "Dear %s, your meter %s has been DISCONNECTED due to non-payment of bill %s " +
                    "which has been overdue for more than %d days. Please clear your outstanding balance to reconnect.",
                    bill.getCustomer().getFullNames(), meter.getMeterNumber(),
                    bill.getBillReference(), DISCONNECTION_THRESHOLD_DAYS);
            notificationService.createNotification(bill.getCustomer(), msg,
                    NotificationType.METER_DISCONNECTED, bill.getId());

            auditService.log("Meter", meter.getId(), "DISCONNECTED",
                    "status=" + oldMeterStatus, "status=DISCONNECTED",
                    "Meter disconnected due to overdue bill: " + bill.getBillReference());
        }

        log.info("Overdue processing complete: {} bills updated to OVERDUE", processed);
        return processed;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Read / List / Delete
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BillResponse getById(Long id) {
        Bill bill = findBill(id);
        ownershipService.assertOwnership(bill.getCustomer().getId(), "Bill");
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional(readOnly = true)
    public BillResponse getByReference(String reference) {
        Bill bill = billRepository.findByBillReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + reference));
        ownershipService.assertOwnership(bill.getCustomer().getId(), "Bill");
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> getAll(Long customerId, BillStatus status, String search, Pageable pageable) {
        // ROLE_CUSTOMER can only see their own bills — override customerId with their own
        Long ownedCustomerId = ownershipService.getOwnedCustomerIdOrNull();
        if (ownedCustomerId != null) {
            customerId = ownedCustomerId; // silently scope to their own data
        }

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
        auditService.log("Bill", id, "DELETE",
                "ref=" + bill.getBillReference() + ",status=" + bill.getStatus(), null,
                "Bill deleted: " + bill.getBillReference());
    }

    public Bill findBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private static String getMonthName(int month) {
        return switch (month) {
            case 1  -> "January";
            case 2  -> "February";
            case 3  -> "March";
            case 4  -> "April";
            case 5  -> "May";
            case 6  -> "June";
            case 7  -> "July";
            case 8  -> "August";
            case 9  -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> "Month " + month;
        };
    }
}

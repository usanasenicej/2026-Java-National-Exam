package com.wasac.ne.service;

import com.wasac.ne.dto.request.CreatePaymentRequest;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.dto.response.PaymentResponse;
import com.wasac.ne.entity.Bill;
import com.wasac.ne.entity.Payment;
import com.wasac.ne.enums.BillStatus;
import com.wasac.ne.enums.NotificationType;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.BillRepository;
import com.wasac.ne.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final OwnershipService ownershipService;

    @Transactional
    public PaymentResponse recordPayment(CreatePaymentRequest request) {
        Bill bill = billRepository.findByBillReference(request.getBillReference())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + request.getBillReference()));

        if (bill.getStatus() == BillStatus.REJECTED) {
            throw new BusinessException("Cannot pay a rejected bill");
        }
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessException("Bill is already fully paid");
        }
        if (bill.getStatus() == BillStatus.PENDING) {
            throw new BusinessException("Bill must be approved before payment can be recorded");
        }
        // APPROVED, PARTIALLY_PAID, OVERDUE are all valid for payment

        if (request.getAmountPaid().compareTo(bill.getOutstandingBalance()) > 0) {
            throw new BusinessException("Payment amount (" + request.getAmountPaid() +
                    " FRW) exceeds outstanding balance (" + bill.getOutstandingBalance() + " FRW)");
        }

        String paymentRef = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = Payment.builder()
                .paymentReference(paymentRef)
                .bill(bill)
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(request.getPaymentDate())
                .build();

        payment = paymentRepository.save(payment);

        BigDecimal prevAmountPaid    = bill.getAmountPaid();
        BigDecimal prevBalance       = bill.getOutstandingBalance();
        BigDecimal newAmountPaid     = prevAmountPaid.add(request.getAmountPaid());
        BigDecimal newBalance        = bill.getTotalAmount().subtract(newAmountPaid);

        bill.setAmountPaid(newAmountPaid);
        bill.setOutstandingBalance(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
            // Full payment confirmation email
            String emailMsg = String.format(
                    "Dear %s,\n\nYour payment of %s FRW has been successfully received.\n\n" +
                    "Bill Reference: %s\n" +
                    "Billing Period: %s/%d\n" +
                    "Total Bill Amount: %s FRW\n" +
                    "Amount Paid: %s FRW\n" +
                    "Outstanding Balance: 0.00 FRW\n\n" +
                    "Your bill is now FULLY PAID. Thank you for your payment.",
                    bill.getCustomer().getFullNames(),
                    request.getAmountPaid(),
                    bill.getBillReference(),
                    getMonthName(bill.getBillingMonth()), bill.getBillingYear(),
                    bill.getTotalAmount(),
                    newAmountPaid);
            emailService.sendBillNotificationEmail(
                    bill.getCustomer().getEmail(),
                    bill.getCustomer().getFullNames(),
                    emailMsg);
            // In-app notification
            notificationService.createNotification(bill.getCustomer(),
                    String.format("Your bill %s has been fully paid. Total paid: %s FRW. Thank you!",
                            bill.getBillReference(), newAmountPaid),
                    NotificationType.PAYMENT_RECEIVED, bill.getId());
        } else if (newBalance.compareTo(BigDecimal.ZERO) > 0) {
            // Partial payment
            if (bill.getStatus() == BillStatus.APPROVED || bill.getStatus() == BillStatus.OVERDUE) {
                bill.setStatus(BillStatus.PARTIALLY_PAID);
            }
            // Partial payment confirmation email
            String partialEmailMsg = String.format(
                    "Dear %s,\n\nYour partial payment of %s FRW has been successfully received.\n\n" +
                    "Bill Reference: %s\n" +
                    "Billing Period: %s/%d\n" +
                    "Total Bill Amount: %s FRW\n" +
                    "Amount Paid This Time: %s FRW\n" +
                    "Total Paid So Far: %s FRW\n" +
                    "Remaining Balance: %s FRW\n\n" +
                    "Please pay the remaining balance before the due date to avoid penalties.",
                    bill.getCustomer().getFullNames(),
                    request.getAmountPaid(),
                    bill.getBillReference(),
                    getMonthName(bill.getBillingMonth()), bill.getBillingYear(),
                    bill.getTotalAmount(),
                    request.getAmountPaid(),
                    newAmountPaid,
                    newBalance);
            emailService.sendBillNotificationEmail(
                    bill.getCustomer().getEmail(),
                    bill.getCustomer().getFullNames(),
                    partialEmailMsg);
            // In-app notification
            notificationService.createNotification(bill.getCustomer(),
                    String.format("Partial payment of %s FRW received for bill %s. Remaining balance: %s FRW.",
                            request.getAmountPaid(), bill.getBillReference(), newBalance),
                    NotificationType.PAYMENT_RECEIVED, bill.getId());
        }

        billRepository.save(bill);

        String oldBillSnapshot = "amountPaid=" + prevAmountPaid + ",balance=" + prevBalance
                + ",status=" + bill.getStatus();
        String newBillSnapshot = "amountPaid=" + newAmountPaid + ",balance=" + newBalance
                + ",status=" + bill.getStatus();

        auditService.log("Payment", payment.getId(), "CREATE",
                null,
                "ref=" + paymentRef + ",amount=" + request.getAmountPaid()
                        + ",method=" + request.getPaymentMethod() + ",billRef=" + bill.getBillReference(),
                "Payment of " + request.getAmountPaid() + " FRW for bill " + bill.getBillReference());

        auditService.log("Bill", bill.getId(), "PAYMENT_APPLIED",
                oldBillSnapshot, newBillSnapshot,
                "Payment applied to bill " + bill.getBillReference() + ": "
                        + request.getAmountPaid() + " FRW, new balance: " + newBalance);

        return EntityMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        Payment payment = findPayment(id);
        ownershipService.assertOwnership(payment.getBill().getCustomer().getId(), "Payment");
        return EntityMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getAll(Long billId, String search, Pageable pageable) {
        // ROLE_CUSTOMER: if no billId filter given, scope to their own payments only
        Long ownedCustomerId = ownershipService.getOwnedCustomerIdOrNull();

        Page<Payment> page;
        if (billId != null) {
            // If a customer provides a billId, verify the bill belongs to them first
            if (ownedCustomerId != null) {
                ownershipService.assertOwnership(
                        billRepository.findById(billId)
                                .orElseThrow(() -> new com.wasac.ne.exception.ResourceNotFoundException(
                                        "Bill not found: " + billId))
                                .getCustomer().getId(),
                        "Bill");
            }
            page = paymentRepository.findByBillId(billId, pageable);
        } else if (ownedCustomerId != null) {
            // Scope ROLE_CUSTOMER to their own payments
            page = paymentRepository.findByBillCustomerId(ownedCustomerId, pageable);
        } else if (search != null && !search.isBlank()) {
            page = paymentRepository.findByPaymentReferenceContainingIgnoreCase(search, pageable);
        } else {
            page = paymentRepository.findAll(pageable);
        }
        return EntityMapper.toPageResponse(page, EntityMapper::toPaymentResponse);
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = findPayment(id);
        auditService.log("Payment", id, "DELETE",
                "ref=" + payment.getPaymentReference() + ",amount=" + payment.getAmountPaid()
                        + ",billRef=" + payment.getBill().getBillReference(),
                null,
                "Payment deleted: " + payment.getPaymentReference());
        paymentRepository.delete(payment);
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    private static String getMonthName(int month) {
        return switch (month) {
            case 1  -> "January";   case 2  -> "February"; case 3  -> "March";
            case 4  -> "April";     case 5  -> "May";       case 6  -> "June";
            case 7  -> "July";      case 8  -> "August";    case 9  -> "September";
            case 10 -> "October";   case 11 -> "November";  case 12 -> "December";
            default -> "Month " + month;
        };
    }
}

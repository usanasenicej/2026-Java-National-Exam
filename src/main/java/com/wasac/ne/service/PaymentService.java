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
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditService auditService;

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

        BigDecimal newAmountPaid = bill.getAmountPaid().add(request.getAmountPaid());
        BigDecimal newBalance = bill.getTotalAmount().subtract(newAmountPaid);

        bill.setAmountPaid(newAmountPaid);
        bill.setOutstandingBalance(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
            String message = String.format("Dear %s,\nYour %d/%d utility bill of %s FRW has been successfully processed.",
                    bill.getCustomer().getFullNames(), bill.getBillingMonth(), bill.getBillingYear(),
                    bill.getTotalAmount());
            notificationService.createNotification(bill.getCustomer(), message,
                    NotificationType.PAYMENT_RECEIVED, bill.getId());
            emailService.sendBillNotificationEmail(bill.getCustomer().getEmail(),
                    bill.getCustomer().getFullNames(), message);
        }

        billRepository.save(bill);

        auditService.log("Payment", payment.getId(), "CREATE",
                "Payment of " + request.getAmountPaid() + " FRW for bill " + bill.getBillReference());

        return EntityMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return EntityMapper.toPaymentResponse(findPayment(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getAll(Long billId, String search, Pageable pageable) {
        Page<Payment> page;
        if (billId != null) {
            page = paymentRepository.findByBillId(billId, pageable);
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
        paymentRepository.delete(payment);
        auditService.log("Payment", id, "DELETE", "Payment deleted");
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }
}

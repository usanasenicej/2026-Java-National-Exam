package com.wasac.ne.repository;

import com.wasac.ne.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    Page<Payment> findByBillId(Long billId, Pageable pageable);

    Page<Payment> findByPaymentReferenceContainingIgnoreCase(String reference, Pageable pageable);
}

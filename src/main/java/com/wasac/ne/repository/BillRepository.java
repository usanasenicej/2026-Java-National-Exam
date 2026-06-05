package com.wasac.ne.repository;

import com.wasac.ne.entity.Bill;
import com.wasac.ne.enums.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillReference(String billReference);

    boolean existsByBillReference(String billReference);

    Page<Bill> findByCustomerId(Long customerId, Pageable pageable);

    Page<Bill> findByStatus(BillStatus status, Pageable pageable);

    Page<Bill> findByBillingMonthAndBillingYear(int month, int year, Pageable pageable);

    Page<Bill> findByBillReferenceContainingIgnoreCase(String reference, Pageable pageable);
}

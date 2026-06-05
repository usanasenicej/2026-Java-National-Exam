package com.wasac.ne.repository;

import com.wasac.ne.entity.Bill;
import com.wasac.ne.enums.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillReference(String billReference);

    boolean existsByBillReference(String billReference);

    Page<Bill> findByCustomerId(Long customerId, Pageable pageable);

    Page<Bill> findByStatus(BillStatus status, Pageable pageable);

    Page<Bill> findByBillingMonthAndBillingYear(int month, int year, Pageable pageable);

    Page<Bill> findByBillReferenceContainingIgnoreCase(String reference, Pageable pageable);

    /**
     * Fetch all APPROVED (unpaid) bills whose due date is before today.
     * These are candidates for penalty application + OVERDUE status.
     */
    @Query("SELECT b FROM Bill b WHERE b.status = :status AND b.dueDate < :today")
    List<Bill> findOverdueBills(@Param("status") BillStatus status, @Param("today") LocalDate today);

    /**
     * Find bills already marked OVERDUE so we can check disconnection threshold.
     */
    @Query("SELECT b FROM Bill b WHERE b.status = 'OVERDUE' AND b.dueDate < :cutoffDate")
    List<Bill> findBillsOverdueSince(@Param("cutoffDate") LocalDate cutoffDate);

    /** Duplicate-bill guard: one bill per meter reading. */
    boolean existsByMeterReadingId(Long meterReadingId);

    /** Duplicate-bill guard: one bill per meter+month+year (via meter reading's meter). */
    @Query("SELECT COUNT(b) > 0 FROM Bill b WHERE b.meterReading.meter.id = :meterId " +
           "AND b.billingMonth = :month AND b.billingYear = :year")
    boolean existsByMeterIdAndBillingMonthAndBillingYear(
            @Param("meterId") Long meterId,
            @Param("month") int month,
            @Param("year") int year);
}

package com.wasac.ne.repository;

import com.wasac.ne.entity.ServiceCharge;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceChargeRepository extends JpaRepository<ServiceCharge, Long> {

    @Query("SELECT s FROM ServiceCharge s WHERE s.meterType = :meterType AND s.status = :status " +
           "AND s.effectiveFrom <= :billingDate AND (s.effectiveTo IS NULL OR s.effectiveTo >= :billingDate) " +
           "ORDER BY s.version DESC")
    List<ServiceCharge> findActiveChargesForDate(@Param("meterType") MeterType meterType,
                                                 @Param("status") Status status,
                                                 @Param("billingDate") LocalDate billingDate,
                                                 Pageable pageable);

    default Optional<ServiceCharge> findActiveForDate(MeterType meterType, Status status, LocalDate billingDate) {
        List<ServiceCharge> results = findActiveChargesForDate(meterType, status, billingDate,
                org.springframework.data.domain.PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    Page<ServiceCharge> findByMeterType(MeterType meterType, Pageable pageable);
}

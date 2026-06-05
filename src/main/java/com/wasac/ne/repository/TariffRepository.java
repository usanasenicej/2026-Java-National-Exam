package com.wasac.ne.repository;

import com.wasac.ne.entity.Tariff;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    @Query("SELECT t FROM Tariff t WHERE t.meterType = :meterType AND t.status = :status " +
           "AND t.effectiveFrom <= :billingDate AND (t.effectiveTo IS NULL OR t.effectiveTo >= :billingDate) " +
           "ORDER BY t.version DESC")
    List<Tariff> findActiveTariffsForDate(@Param("meterType") MeterType meterType,
                                          @Param("status") Status status,
                                          @Param("billingDate") LocalDate billingDate,
                                          Pageable pageable);

    default Optional<Tariff> findActiveTariffForDate(MeterType meterType, Status status, LocalDate billingDate) {
        List<Tariff> results = findActiveTariffsForDate(meterType, status, billingDate,
                org.springframework.data.domain.PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    Page<Tariff> findByMeterType(MeterType meterType, Pageable pageable);

    Optional<Tariff> findTopByMeterTypeOrderByVersionDesc(MeterType meterType);
}

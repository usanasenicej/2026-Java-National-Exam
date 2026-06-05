package com.wasac.ne.repository;

import com.wasac.ne.entity.TaxConfig;
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

public interface TaxConfigRepository extends JpaRepository<TaxConfig, Long> {

    @Query("SELECT t FROM TaxConfig t WHERE t.status = :status " +
           "AND t.effectiveFrom <= :billingDate AND (t.effectiveTo IS NULL OR t.effectiveTo >= :billingDate) " +
           "ORDER BY t.version DESC")
    List<TaxConfig> findActiveTaxesForDate(@Param("status") Status status,
                                           @Param("billingDate") LocalDate billingDate,
                                           Pageable pageable);

    default Optional<TaxConfig> findActiveForDate(Status status, LocalDate billingDate) {
        List<TaxConfig> results = findActiveTaxesForDate(status, billingDate,
                org.springframework.data.domain.PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    Page<TaxConfig> findByStatus(Status status, Pageable pageable);
}

package com.wasac.ne.repository;

import com.wasac.ne.entity.PenaltyConfig;
import com.wasac.ne.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PenaltyConfigRepository extends JpaRepository<PenaltyConfig, Long> {

    @Query("SELECT p FROM PenaltyConfig p WHERE p.status = :status " +
           "AND p.effectiveFrom <= :billingDate AND (p.effectiveTo IS NULL OR p.effectiveTo >= :billingDate) " +
           "ORDER BY p.version DESC LIMIT 1")
    Optional<PenaltyConfig> findActiveForDate(@Param("status") Status status,
                                              @Param("billingDate") LocalDate billingDate);

    Page<PenaltyConfig> findByStatus(Status status, Pageable pageable);
}

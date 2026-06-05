package com.wasac.ne.repository;

import com.wasac.ne.entity.Meter;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeterRepository extends JpaRepository<Meter, Long> {

    boolean existsByMeterNumber(String meterNumber);

    Optional<Meter> findByMeterNumber(String meterNumber);

    Page<Meter> findByMeterNumberContainingIgnoreCase(String meterNumber, Pageable pageable);

    List<Meter> findByCustomerId(Long customerId);

    Page<Meter> findByCustomerId(Long customerId, Pageable pageable);

    Page<Meter> findByMeterTypeAndStatus(MeterType meterType, Status status, Pageable pageable);
}

package com.wasac.ne.repository;

import com.wasac.ne.entity.MeterReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    boolean existsByMeterIdAndReadingMonthAndReadingYear(Long meterId, int month, int year);

    Optional<MeterReading> findTopByMeterIdOrderByReadingDateDesc(Long meterId);

    Page<MeterReading> findByMeterId(Long meterId, Pageable pageable);

    Page<MeterReading> findByReadingMonthAndReadingYear(int month, int year, Pageable pageable);
}

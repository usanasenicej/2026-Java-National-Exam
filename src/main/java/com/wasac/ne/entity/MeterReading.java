package com.wasac.ne.entity;

import com.wasac.ne.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "meter_readings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"meter_id", "reading_month", "reading_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReading extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal previousReading;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentReading;

    @Column(nullable = false)
    private LocalDate readingDate;

    @Column(nullable = false)
    private int readingMonth;

    @Column(nullable = false)
    private int readingYear;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal consumption;
}

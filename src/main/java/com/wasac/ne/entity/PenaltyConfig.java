package com.wasac.ne.entity;

import com.wasac.ne.entity.base.AuditableEntity;
import com.wasac.ne.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "penalty_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(nullable = false)
    private int gracePeriodDays;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
}

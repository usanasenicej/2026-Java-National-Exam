package com.wasac.ne.entity;

import com.wasac.ne.entity.base.AuditableEntity;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "service_charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCharge extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterType meterType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
}

package com.wasac.ne.entity;

import com.wasac.ne.entity.base.AuditableEntity;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.TariffType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tariffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tariff extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterType meterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TariffType tariffType;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(precision = 12, scale = 2)
    private BigDecimal flatRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "tariff", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TariffTier> tiers = new ArrayList<>();
}

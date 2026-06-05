package com.wasac.ne.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tariff_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    @Column(nullable = false)
    private int tierOrder;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minUnits;

    private BigDecimal maxUnits;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerUnit;
}

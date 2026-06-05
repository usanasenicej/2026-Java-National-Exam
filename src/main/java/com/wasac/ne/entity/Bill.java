package com.wasac.ne.entity;

import com.wasac.ne.entity.base.AuditableEntity;
import com.wasac.ne.enums.BillStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String billReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_reading_id")
    private MeterReading meterReading;

    @Column(nullable = false)
    private int billingMonth;

    @Column(nullable = false)
    private int billingYear;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal consumptionAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal serviceChargeAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amountPaid;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillStatus status;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate approvedAt;

    private String approvedBy;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();
}

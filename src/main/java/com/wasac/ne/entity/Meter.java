package com.wasac.ne.entity;

import com.wasac.ne.entity.base.AuditableEntity;
import com.wasac.ne.enums.MeterType;
import com.wasac.ne.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meter extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String meterNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterType meterType;

    @Column(nullable = false)
    private LocalDate installationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "meter", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MeterReading> readings = new ArrayList<>();
}

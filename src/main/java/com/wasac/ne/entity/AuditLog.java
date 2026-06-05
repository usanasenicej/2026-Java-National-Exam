package com.wasac.ne.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityName;

    private Long entityId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String performedBy;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}

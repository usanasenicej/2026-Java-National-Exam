package com.wasac.ne.repository;

import com.wasac.ne.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityNameContainingIgnoreCase(String entityName, Pageable pageable);

    Page<AuditLog> findByPerformedByContainingIgnoreCase(String performedBy, Pageable pageable);
}

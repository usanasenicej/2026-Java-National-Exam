package com.wasac.ne.service;

import com.wasac.ne.entity.AuditLog;
import com.wasac.ne.repository.AuditLogRepository;
import com.wasac.ne.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /** Basic log without old/new value tracking */
    public void log(String entityName, Long entityId, String action, String details) {
        log(entityName, entityId, action, null, null, details);
    }

    /**
     * Full audit log capturing old and new values — required for update/change operations.
     *
     * @param entityName  entity class name (e.g. "Customer", "Bill")
     * @param entityId    PK of the affected record
     * @param action      verb: CREATE | UPDATE | DELETE | APPROVE | REJECT | GENERATE | ...
     * @param oldValue    serialised snapshot of the record before the change (nullable)
     * @param newValue    serialised snapshot of the record after the change (nullable)
     * @param details     human-readable description
     */
    public void log(String entityName, Long entityId, String action,
                    String oldValue, String newValue, String details) {
        String performedBy = SecurityUtils.getCurrentUserEmail();
        AuditLog auditLog = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy)
                .oldValue(oldValue)
                .newValue(newValue)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
        log.info("Audit: [{}] {} id={} by={}", action, entityName, entityId, performedBy);
    }
}

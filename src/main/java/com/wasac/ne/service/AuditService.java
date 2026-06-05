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

    public void log(String entityName, Long entityId, String action, String details) {
        String performedBy = SecurityUtils.getCurrentUserEmail();
        AuditLog auditLog = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
        log.info("Audit: {} {} on {} by {}", action, entityName, entityId, performedBy);
    }
}

package com.wasac.ne.service;

import com.wasac.ne.dto.response.AuditLogResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAll(String search, Pageable pageable) {
        Page<com.wasac.ne.entity.AuditLog> page;
        if (search != null && !search.isBlank()) {
            page = auditLogRepository.findByEntityNameContainingIgnoreCase(search, pageable);
        } else {
            page = auditLogRepository.findAll(pageable);
        }
        return EntityMapper.toPageResponse(page, EntityMapper::toAuditLogResponse);
    }
}

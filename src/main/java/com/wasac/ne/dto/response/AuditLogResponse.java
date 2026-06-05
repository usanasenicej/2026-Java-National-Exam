package com.wasac.ne.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {

    private Long id;
    private String entityName;
    private Long entityId;
    private String action;
    private String performedBy;
    private String oldValue;
    private String newValue;
    private String details;
    private LocalDateTime timestamp;
}

package com.wasac.ne.dto.response;

import com.wasac.ne.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private String message;
    private NotificationType type;
    private boolean readFlag;
    private Long billId;
    private LocalDateTime createdAt;
}

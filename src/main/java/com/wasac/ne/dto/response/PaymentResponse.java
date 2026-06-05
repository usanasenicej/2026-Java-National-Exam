package com.wasac.ne.dto.response;

import com.wasac.ne.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Long id;
    private String paymentReference;
    private String billReference;
    private Long billId;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private LocalDateTime createdAt;
}

package com.wasac.ne.dto.response;

import com.wasac.ne.enums.BillStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BillResponse {

    private Long id;
    private String billReference;
    private Long customerId;
    private String customerName;
    private Long meterReadingId;
    private int billingMonth;
    private int billingYear;
    private BigDecimal consumptionAmount;
    private BigDecimal serviceChargeAmount;
    private BigDecimal taxAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal outstandingBalance;
    private BillStatus status;
    private LocalDate dueDate;
    private LocalDate approvedAt;
    private String approvedBy;
    private LocalDateTime createdAt;
}

package com.wasac.ne.mapper;

import com.wasac.ne.dto.response.*;
import com.wasac.ne.entity.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullNames(user.getFullNames())
                .nationalId(user.getNationalId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(user.getRoles())
                .emailVerified(user.isEmailVerified())
                .mustChangePassword(user.isMustChangePassword())
                .customerId(user.getCustomer() != null ? user.getCustomer().getId() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullNames(customer.getFullNames())
                .nationalId(customer.getNationalId())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .dateOfBirth(customer.getDateOfBirth())
                .address(customer.getAddress())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public static MeterResponse toMeterResponse(Meter meter) {
        return MeterResponse.builder()
                .id(meter.getId())
                .meterNumber(meter.getMeterNumber())
                .meterType(meter.getMeterType())
                .installationDate(meter.getInstallationDate())
                .status(meter.getStatus())
                .customerId(meter.getCustomer().getId())
                .customerName(meter.getCustomer().getFullNames())
                .createdAt(meter.getCreatedAt())
                .build();
    }

    public static MeterReadingResponse toMeterReadingResponse(MeterReading reading) {
        return MeterReadingResponse.builder()
                .id(reading.getId())
                .meterId(reading.getMeter().getId())
                .meterNumber(reading.getMeter().getMeterNumber())
                .previousReading(reading.getPreviousReading())
                .currentReading(reading.getCurrentReading())
                .consumption(reading.getConsumption())
                .readingDate(reading.getReadingDate())
                .readingMonth(reading.getReadingMonth())
                .readingYear(reading.getReadingYear())
                .createdAt(reading.getCreatedAt())
                .build();
    }

    public static TariffResponse toTariffResponse(Tariff tariff) {
        List<TariffTierResponse> tiers = tariff.getTiers().stream()
                .map(t -> TariffTierResponse.builder()
                        .id(t.getId())
                        .tierOrder(t.getTierOrder())
                        .minUnits(t.getMinUnits())
                        .maxUnits(t.getMaxUnits())
                        .ratePerUnit(t.getRatePerUnit())
                        .build())
                .collect(Collectors.toList());

        return TariffResponse.builder()
                .id(tariff.getId())
                .name(tariff.getName())
                .meterType(tariff.getMeterType())
                .tariffType(tariff.getTariffType())
                .version(tariff.getVersion())
                .effectiveFrom(tariff.getEffectiveFrom())
                .effectiveTo(tariff.getEffectiveTo())
                .flatRate(tariff.getFlatRate())
                .serviceChargeAmount(tariff.getServiceChargeAmount())
                .vatPercentage(tariff.getVatPercentage())
                .latePenaltyPercentage(tariff.getLatePenaltyPercentage())
                .status(tariff.getStatus())
                .tiers(tiers)
                .createdAt(tariff.getCreatedAt())
                .build();
    }

    public static BillResponse toBillResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .billReference(bill.getBillReference())
                .customerId(bill.getCustomer().getId())
                .customerName(bill.getCustomer().getFullNames())
                .meterReadingId(bill.getMeterReading() != null ? bill.getMeterReading().getId() : null)
                .billingMonth(bill.getBillingMonth())
                .billingYear(bill.getBillingYear())
                .consumptionAmount(bill.getConsumptionAmount())
                .serviceChargeAmount(bill.getServiceChargeAmount())
                .taxAmount(bill.getTaxAmount())
                .penaltyAmount(bill.getPenaltyAmount())
                .totalAmount(bill.getTotalAmount())
                .amountPaid(bill.getAmountPaid())
                .outstandingBalance(bill.getOutstandingBalance())
                .status(bill.getStatus())
                .dueDate(bill.getDueDate())
                .approvedAt(bill.getApprovedAt())
                .approvedBy(bill.getApprovedBy())
                .createdAt(bill.getCreatedAt())
                .build();
    }

    public static PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .billReference(payment.getBill().getBillReference())
                .billId(payment.getBill().getId())
                .amountPaid(payment.getAmountPaid())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public static NotificationResponse toNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .customerId(notification.getCustomer().getId())
                .customerName(notification.getCustomer().getFullNames())
                .message(notification.getMessage())
                .type(notification.getType())
                .readFlag(notification.isReadFlag())
                .billId(notification.getBillId())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public static AuditLogResponse toAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .performedBy(log.getPerformedBy())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }

    public static <T, R> PageResponse<R> toPageResponse(Page<T> page, java.util.function.Function<T, R> mapper) {
        return PageResponse.<R>builder()
                .content(page.getContent().stream().map(mapper).collect(Collectors.toList()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

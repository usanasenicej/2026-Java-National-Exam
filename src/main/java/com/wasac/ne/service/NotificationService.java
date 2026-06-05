package com.wasac.ne.service;

import com.wasac.ne.dto.response.NotificationResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.entity.Customer;
import com.wasac.ne.entity.Notification;
import com.wasac.ne.enums.NotificationType;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(Customer customer, String message, NotificationType type, Long billId) {
        Notification notification = Notification.builder()
                .customer(customer)
                .message(message)
                .type(type)
                .readFlag(false)
                .billId(billId)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getByCustomer(Long customerId, Boolean read, Pageable pageable) {
        Page<Notification> page = read != null
                ? notificationRepository.findByCustomerIdAndReadFlag(customerId, read, pageable)
                : notificationRepository.findByCustomerId(customerId, pageable);
        return EntityMapper.toPageResponse(page, EntityMapper::toNotificationResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAll(Pageable pageable) {
        return EntityMapper.toPageResponse(notificationRepository.findAll(pageable),
                EntityMapper::toNotificationResponse);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setReadFlag(true);
        return EntityMapper.toNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }
}

package com.wasac.ne.repository;

import com.wasac.ne.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByCustomerId(Long customerId, Pageable pageable);

    Page<Notification> findByCustomerIdAndReadFlag(Long customerId, boolean readFlag, Pageable pageable);
}

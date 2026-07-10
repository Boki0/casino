package com.boki0.casino.notification.repository;

import com.boki0.casino.notification.entity.EmailDelivery;
import com.boki0.casino.notification.entity.EmailDeliveryStatus;
import com.boki0.casino.notification.entity.EmailType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, UUID> {

    Optional<EmailDelivery> findByEventIdAndEmailType(UUID eventId, EmailType emailType);

    boolean existsByEventIdAndEmailType(UUID eventId, EmailType emailType);

    List<EmailDelivery> findByStatusOrderByCreatedAtAsc(EmailDeliveryStatus status);
}

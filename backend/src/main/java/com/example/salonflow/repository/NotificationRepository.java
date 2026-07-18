package com.example.salonflow.repository;

import com.example.salonflow.entity.Notification;
import com.example.salonflow.entity.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndStatus(Long recipientId, NotificationStatus status);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);
}

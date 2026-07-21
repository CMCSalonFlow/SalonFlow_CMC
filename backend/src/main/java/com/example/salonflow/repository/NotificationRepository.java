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

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.status = com.example.salonflow.entity.enums.NotificationStatus.READ, n.readAt = :readAt WHERE n.recipient.id = :recipientId AND n.status <> com.example.salonflow.entity.enums.NotificationStatus.READ")
    int markAllAsReadByRecipientId(@org.springframework.data.repository.query.Param("recipientId") Long recipientId, @org.springframework.data.repository.query.Param("readAt") java.time.Instant readAt);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM notifications WHERE recipient_user_id = :recipientId AND id NOT IN (" +
            "SELECT id FROM (" +
            "  SELECT id FROM notifications WHERE recipient_user_id = :recipientId ORDER BY created_at DESC LIMIT 100" +
            ") AS subquery)", nativeQuery = true)
    void trimOldNotifications(@org.springframework.data.repository.query.Param("recipientId") Long recipientId);
}

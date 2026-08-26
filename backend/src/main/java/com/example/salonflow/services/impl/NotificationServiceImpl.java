package com.example.salonflow.services.impl;

import com.example.salonflow.dto.notification.NotificationResponse;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.Notification;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.enums.NotificationChannel;
import com.example.salonflow.entity.enums.NotificationStatus;
import com.example.salonflow.notification.BookingNotificationEvent;
import com.example.salonflow.notification.sender.NotificationSender;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.NotificationRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.services.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.example.salonflow.websocket.NotificationWebSocketHandler;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final List<NotificationSender> notificationSenders;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    private Map<NotificationChannel, NotificationSender> senderMap() {
        Map<NotificationChannel, NotificationSender> map = new EnumMap<>(NotificationChannel.class);
        for (NotificationSender sender : notificationSenders) {
            map.put(sender.getChannel(), sender);
        }
        return map;
    }

    @Override
    @Transactional
    public void handleBookingEvent(BookingNotificationEvent event) {
        if (event.recipientUserIds() == null || event.recipientUserIds().isEmpty()) {
            return;
        }

        Map<NotificationChannel, NotificationSender> senders = senderMap();
        Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);

        for (Long recipientId : event.recipientUserIds().stream().distinct().toList()) {
            User recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) {
                log.warn("Skip notification for missing userId={}", recipientId);
                continue;
            }

            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .booking(booking)
                    .channel(NotificationChannel.IN_APP)
                    .status(NotificationStatus.UNREAD)
                    .title(event.title())
                    .message(event.message())
                    .payloadJson(event.payloadJson())
                    .sourceType("BOOKING")
                    .sourceId(event.bookingId())
                    .eventType(event.type().name())
                    .build();

            notification = notificationRepository.save(notification);

            // Trim old notifications exceeding 100 per recipient
            try {
                notificationRepository.trimOldNotifications(recipientId);
            } catch (Exception ex) {
                log.warn("Failed to trim notifications for userId={}: {}", recipientId, ex.getMessage());
            }

            NotificationResponse response = toResponse(notification);
            long unreadCount = countUnread(recipientId);

            // Real-time WebSocket emission
            try {
                notificationWebSocketHandler.sendNotificationToUser(recipientId, response, unreadCount);
            } catch (Exception ex) {
                log.error("Failed to emit WebSocket notification for userId={}: {}", recipientId, ex.getMessage());
            }

            NotificationSender inAppSender = senders.get(NotificationChannel.IN_APP);
            if (inAppSender != null) {
                inAppSender.send(notification);
            }

            NotificationSender pushSender = senders.get(NotificationChannel.PUSH);
            if (pushSender != null) {
                try {
                    pushSender.send(notification);
                } catch (Exception e) {
                    log.error("Failed to prepare push notification for notification id={}", notification.getId(), e);
                }
            }

            if (event.type().name().equals("BOOKING_CREATED")
                    || event.type().name().equals("BOOKING_CANCELLED")
                    || event.type().name().equals("APPOINTMENT_REMINDER")) {
                NotificationSender emailSender = senders.get(NotificationChannel.EMAIL);
                if (emailSender != null) {
                    try {
                        emailSender.send(notification);
                    } catch (Exception e) {
                        log.error("Failed to send email notification for notification id={}", notification.getId(), e);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(100)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification với id: " + notificationId));

        if (notification.getStatus() != NotificationStatus.READ) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(java.time.Instant.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByRecipientId(userId, java.time.Instant.now());
    }

    private NotificationResponse toResponse(Notification notification) {
        Long recipientId = notification.getRecipient() != null ? notification.getRecipient().getId() : null;
        boolean isRead = notification.getStatus() == NotificationStatus.READ;
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(recipientId)
                .userId(recipientId)
                .bookingId(notification.getBooking() != null ? notification.getBooking().getId() : null)
                .channel(notification.getChannel() != null ? notification.getChannel().name() : null)
                .status(notification.getStatus() != null ? notification.getStatus().name() : null)
                .isRead(isRead)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .body(notification.getMessage())
                .payloadJson(notification.getPayloadJson())
                .data(notification.getPayloadJson())
                .sourceType(notification.getSourceType())
                .sourceId(notification.getSourceId())
                .eventType(notification.getEventType())
                .type(notification.getEventType())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

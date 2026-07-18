package com.example.salonflow.notification;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.Notification;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.enums.NotificationChannel;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.NotificationRepository;
import com.example.salonflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBookingNotification(BookingNotificationEvent event) {
        if (event.recipientUserIds() == null || event.recipientUserIds().isEmpty()) {
            log.debug("Skip booking notification {} because there is no recipient", event.type());
            return;
        }

        Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);
        List<Notification> notifications = event.recipientUserIds().stream()
                .distinct()
                .map(userId -> buildNotification(event, booking, userId))
                .flatMap(java.util.Optional::stream)
                .toList();

        if (notifications.isEmpty()) {
            log.debug("No notification persisted for booking {}", event.bookingId());
            return;
        }

        notificationRepository.saveAll(notifications);
        log.info("Persisted {} notification history record(s) for booking {}", notifications.size(), event.bookingId());
    }

    private java.util.Optional<Notification> buildNotification(
            BookingNotificationEvent event,
            Booking booking,
            Long userId
    ) {
        User recipient = userRepository.findById(userId).orElse(null);
        if (recipient == null) {
            log.warn("Skip notification for missing recipient userId={}", userId);
            return java.util.Optional.empty();
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .booking(booking)
                .channel(NotificationChannel.IN_APP)
                .title(event.title())
                .message(event.message())
                .payloadJson(event.payloadJson())
                .sourceType("BOOKING")
                .sourceId(event.bookingId())
                .eventType(event.type().name())
                .build();
        return java.util.Optional.of(notification);
    }
}

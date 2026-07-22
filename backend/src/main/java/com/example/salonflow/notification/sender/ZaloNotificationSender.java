package com.example.salonflow.notification.sender;

import com.example.salonflow.entity.Notification;
import com.example.salonflow.entity.enums.NotificationChannel;
import com.example.salonflow.services.service.ZaloZnsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ZaloNotificationSender implements NotificationSender {

    private final ZaloZnsService zaloZnsService;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.ZALO;
    }

    @Override
    public void send(Notification notification) {
        if (notification == null || notification.getBooking() == null) {
            log.warn("Skip Zalo notification for empty booking/notification payload");
            return;
        }

        log.info("Sending Zalo Notification for event: {}, bookingId: {}", notification.getEventType(), notification.getBooking().getId());

        String eventType = notification.getEventType() != null ? notification.getEventType() : "";
        switch (eventType) {
            case "BOOKING_CREATED":
                zaloZnsService.sendBookingCreatedZns(notification.getBooking(), notification.getRecipient());
                break;
            case "APPOINTMENT_REMINDER":
                zaloZnsService.sendAppointmentReminderZns(notification.getBooking(), notification.getRecipient());
                break;
            case "BOOKING_CANCELLED":
                zaloZnsService.sendBookingCancelledZns(notification.getBooking(), notification.getRecipient(), notification.getMessage());
                break;
            default:
                zaloZnsService.sendBookingCreatedZns(notification.getBooking(), notification.getRecipient());
                break;
        }
    }
}

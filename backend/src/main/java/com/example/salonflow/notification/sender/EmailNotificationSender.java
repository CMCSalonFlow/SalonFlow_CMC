package com.example.salonflow.notification.sender;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.Notification;
import com.example.salonflow.entity.enums.NotificationChannel;
import com.example.salonflow.notification.BookingNotificationType;
import com.example.salonflow.notification.email.BookingEmailTemplateService;
import com.example.salonflow.services.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.notification.email", name = "enabled", havingValue = "true")
public class EmailNotificationSender implements NotificationSender {

    private final EmailService emailService;
    private final BookingEmailTemplateService bookingEmailTemplateService;
    private final ObjectMapper objectMapper;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        if (notification.getRecipient() == null || notification.getRecipient().getEmail() == null) {
            log.debug("Skip email notification {} because recipient email is missing", notification.getId());
            return;
        }

        String eventType = notification.getEventType() != null ? notification.getEventType().toUpperCase() : "";
        String to = notification.getRecipient().getEmail();
        String subject = notification.getTitle();
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;padding:24px;color:#2c221d;">
                  <h2>%s</h2>
                  <p>%s</p>
                </div>
                """.formatted(notification.getTitle(), notification.getMessage());

        Booking booking = notification.getBooking();
        if (booking != null) {
            if (BookingNotificationType.BOOKING_CREATED.name().equals(eventType)) {
                subject = "Xác nhận đặt lịch #" + booking.getId();
                html = bookingEmailTemplateService.renderBookingConfirmation(booking);
            } else if (BookingNotificationType.APPOINTMENT_REMINDER.name().equals(eventType)) {
                subject = "Nhắc lịch hẹn #" + booking.getId();
                html = bookingEmailTemplateService.renderAppointmentReminder(booking);
            } else if (BookingNotificationType.BOOKING_CANCELLED.name().equals(eventType)) {
                subject = "Hủy lịch hẹn #" + booking.getId();
                html = bookingEmailTemplateService.renderBookingCancellation(booking, extractCancellationReason(notification));
            }
        }

        emailService.sendNotificationEmail(to, subject, html);
    }

    private String extractCancellationReason(Notification notification) {
        if (notification.getMessage() != null && !notification.getMessage().isBlank()) {
            return notification.getMessage();
        }

        if (notification.getPayloadJson() == null || notification.getPayloadJson().isBlank()) {
            return "";
        }

        try {
            Map<?, ?> payload = objectMapper.readValue(notification.getPayloadJson(), Map.class);
            Object reason = payload.get("cancellationReason");
            return reason != null ? reason.toString() : "";
        } catch (Exception e) {
            log.debug("Failed to parse cancellation reason from notification {}: {}", notification.getId(), e.getMessage());
            return "";
        }
    }
}

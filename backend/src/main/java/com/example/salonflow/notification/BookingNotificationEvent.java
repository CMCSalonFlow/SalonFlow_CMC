package com.example.salonflow.notification;

import java.util.List;

public record BookingNotificationEvent(
        Long bookingId,
        Long branchId,
        Long customerId,
        List<Long> recipientUserIds,
        BookingNotificationType type,
        String title,
        String message,
        String payloadJson
) {
}

package com.example.salonflow.services.service;

import com.example.salonflow.dto.notification.NotificationResponse;
import com.example.salonflow.notification.BookingNotificationEvent;

import java.util.List;

public interface NotificationService {

    void handleBookingEvent(BookingNotificationEvent event);

    List<NotificationResponse> getMyNotifications(Long userId);

    long countUnread(Long userId);

    NotificationResponse markAsRead(Long userId, Long notificationId);
}

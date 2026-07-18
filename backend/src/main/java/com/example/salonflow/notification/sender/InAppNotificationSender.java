package com.example.salonflow.notification.sender;

import com.example.salonflow.entity.Notification;
import com.example.salonflow.entity.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InAppNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {
        log.debug("Persisted in-app notification id={} recipientId={}", notification.getId(),
                notification.getRecipient() != null ? notification.getRecipient().getId() : null);
    }
}

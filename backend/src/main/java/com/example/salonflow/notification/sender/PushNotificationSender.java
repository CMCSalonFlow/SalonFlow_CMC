package com.example.salonflow.notification.sender;

import com.example.salonflow.entity.UserFcmToken;
import com.example.salonflow.entity.Notification;
import com.example.salonflow.entity.enums.NotificationChannel;
import com.example.salonflow.services.service.FcmTokenService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PushNotificationSender implements NotificationSender {

    private final FcmTokenService fcmTokenService;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(Notification notification) {
        if (notification.getRecipient() == null || notification.getRecipient().getId() == null) {
            log.debug("Skip push notification {} because recipient is missing", notification.getId());
            return;
        }

        Long userId = notification.getRecipient().getId();
        java.util.List<UserFcmToken> activeTokens = fcmTokenService.getActiveTokensForUser(userId);

        if (activeTokens.isEmpty()) {
            log.debug("No active FCM token for userId={} notificationId={}", userId, notification.getId());
            return;
        }

        log.info(
                "Prepared push notification id={} for userId={} tokenCount={} title={}",
                notification.getId(),
                userId,
                activeTokens.size(),
                notification.getTitle()
        );
    }
}

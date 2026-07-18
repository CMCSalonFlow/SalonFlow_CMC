package com.example.salonflow.notification.sender;

import com.example.salonflow.entity.Notification;
import com.example.salonflow.entity.enums.NotificationChannel;
import com.example.salonflow.services.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.notification.email", name = "enabled", havingValue = "true")
public class EmailNotificationSender implements NotificationSender {

    private final EmailService emailService;

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

        emailService.sendNotificationEmail(
                notification.getRecipient().getEmail(),
                notification.getTitle(),
                """
                <h2>%s</h2>
                <p>%s</p>
                """.formatted(notification.getTitle(), notification.getMessage())
        );
    }
}

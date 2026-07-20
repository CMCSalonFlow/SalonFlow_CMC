package com.example.salonflow.notification.sender;

import com.example.salonflow.entity.Notification;
import com.example.salonflow.entity.enums.NotificationChannel;

public interface NotificationSender {

    NotificationChannel getChannel();

    void send(Notification notification);
}

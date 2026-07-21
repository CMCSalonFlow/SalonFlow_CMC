package com.example.salonflow.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private Long recipientId;
    private Long userId; // Alias for recipientId
    private Long bookingId;
    private String channel;
    private String status;
    private Boolean isRead; // Alias for status == READ
    private String title;
    private String message;
    private String body; // Alias for message
    private String payloadJson;
    private String data; // Alias for payloadJson
    private String sourceType;
    private Long sourceId;
    private String eventType;
    private String type; // Alias for eventType
    private Instant readAt;
    private Instant createdAt;
}

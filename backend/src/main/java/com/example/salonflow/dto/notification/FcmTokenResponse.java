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
public class FcmTokenResponse {

    private Long id;
    private Long userId;
    private String token;
    private String deviceName;
    private String platform;
    private Boolean isActive;
    private Instant lastSeenAt;
    private Instant createdAt;
    private Instant updatedAt;
}

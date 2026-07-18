package com.example.salonflow.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmTokenRegisterRequest {

    @NotBlank(message = "FCM token không được để trống")
    private String token;

    private String deviceName;

    private String platform;
}

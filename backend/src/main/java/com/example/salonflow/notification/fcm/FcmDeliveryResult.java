package com.example.salonflow.notification.fcm;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FcmDeliveryResult {

    private final boolean success;
    private final boolean invalidToken;
    private final int statusCode;
    private final String responseBody;
}

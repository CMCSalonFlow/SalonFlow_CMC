package com.example.salonflow.notification.fcm;

import java.util.Map;

public interface FcmPushClient {

    FcmDeliveryResult sendToToken(String deviceToken, String title, String body, Map<String, String> data);
}

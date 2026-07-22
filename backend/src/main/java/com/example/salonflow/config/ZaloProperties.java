package com.example.salonflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "zalo")
@Getter
@Setter
public class ZaloProperties {
    private String appId;
    private String appSecret;
    private String oaId;
    private String refreshToken;
    private boolean mockEnable = true;
    private String testPhone;
    private String tokenUrl = "https://oauth.zaloapp.com/v4/oa/access_token";
    private String znsSendUrl = "https://business.openapi.zalo.me/message/template";
    
    private Template template = new Template();

    @Getter
    @Setter
    public static class Template {
        private String bookingCreated = "300123";
        private String appointmentReminder = "300124";
        private String bookingCancelled = "300125";
    }
}

package com.example.salonflow.notification.mail;

public interface EmailProvider {

    String getName();

    boolean isConfigured();

    void send(String to, String subject, String html);
}

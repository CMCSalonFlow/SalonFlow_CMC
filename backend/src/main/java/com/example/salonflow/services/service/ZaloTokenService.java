package com.example.salonflow.services.service;

public interface ZaloTokenService {
    /**
     * Get valid Zalo OA Access Token (auto refresh if expired).
     */
    String getValidAccessToken();

    /**
     * Refresh Zalo Access Token manually or via cron.
     */
    String refreshAccessToken();
}

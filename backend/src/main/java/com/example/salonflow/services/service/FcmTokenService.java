package com.example.salonflow.services.service;

import com.example.salonflow.dto.notification.FcmTokenRegisterRequest;
import com.example.salonflow.dto.notification.FcmTokenResponse;
import com.example.salonflow.entity.UserFcmToken;

import java.util.List;

public interface FcmTokenService {

    FcmTokenResponse registerToken(Long userId, FcmTokenRegisterRequest request);

    void revokeToken(Long userId, String token);

    List<FcmTokenResponse> getMyTokens(Long userId);

    List<UserFcmToken> getActiveTokensForUser(Long userId);
}

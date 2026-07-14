package com.example.salonflow.services.service;

import com.example.salonflow.entity.RefreshToken;
import com.example.salonflow.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyExpiration(String token);

    void deleteByUser(User user);
}

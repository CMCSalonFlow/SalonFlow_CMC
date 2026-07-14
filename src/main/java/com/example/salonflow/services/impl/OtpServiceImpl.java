package com.example.salonflow.services.impl;

import com.example.salonflow.services.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl
        implements OtpService {

    private final StringRedisTemplate redisTemplate;

    private static final long OTP_TTL = 5;

    @Override
    public void saveOtp(
            String email,
            String otp
    ) {

        redisTemplate.opsForValue().set(
                "VERIFY_EMAIL:" + email,
                otp,
                OTP_TTL,
                TimeUnit.MINUTES
        );
    }

    @Override
    public String getOtp(
            String email
    ) {

        return redisTemplate.opsForValue().get(
                "VERIFY_EMAIL:" + email
        );
    }

    @Override
    public void deleteOtp(
            String email
    ) {

        redisTemplate.delete(
                "VERIFY_EMAIL:" + email
        );
    }
}

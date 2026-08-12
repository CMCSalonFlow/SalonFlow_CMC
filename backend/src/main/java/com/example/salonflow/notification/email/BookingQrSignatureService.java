package com.example.salonflow.notification.email;

import com.example.salonflow.entity.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
@Slf4j
public class BookingQrSignatureService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${booking.qr.secret:${jwt.secret}}")
    private String secret;

    public String generateSignature(Booking booking) {
        if (booking == null || booking.getId() == null) {
            throw new IllegalArgumentException("Booking không hợp lệ để tạo QR signature");
        }
        return sign(buildPayload(booking.getId()));
    }

    public boolean verify(Long bookingId, String signature) {
        if (bookingId == null || signature == null || signature.isBlank()) {
            return false;
        }

        String expected = sign(buildPayload(bookingId));
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = signature.trim().getBytes(StandardCharsets.UTF_8);
        return expectedBytes.length == actualBytes.length && MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String buildPayload(Long bookingId) {
        return "booking-checkin:" + bookingId;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            log.error("Failed to sign booking QR payload", e);
            throw new IllegalStateException("Không thể tạo chữ ký QR booking", e);
        }
    }
}

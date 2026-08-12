package com.example.salonflow.notification.email;

import com.example.salonflow.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingQrCodeService {

    private final BookingQrSignatureService signatureService;

    @Value("${frontend.url:}")
    private String frontendUrl;

    public String generateDataUrl(Booking booking) {
        try {
            String content = generateCheckInContent(booking);
            return "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data="
                    + URLEncoder.encode(content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to generate QR code for booking {}: {}", booking != null ? booking.getId() : null, e.getMessage());
            return "";
        }
    }

    public String generateCheckInContent(Booking booking) {
        String signature = signatureService.generateSignature(booking);
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            return frontendUrl.replaceAll("/+$", "")
                    + "/check-in?bookingId=" + booking.getId()
                    + "&signature=" + URLEncoder.encode(signature, StandardCharsets.UTF_8);
        }

        return "SalonFlow|bookingId=" + booking.getId() + "|signature=" + signature;
    }
}

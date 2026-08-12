package com.example.salonflow.notification.email;

import com.example.salonflow.entity.Booking;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 220, 220);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
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

package com.example.salonflow.notification.email;

import com.example.salonflow.entity.Booking;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
@Slf4j
public class BookingQrCodeService {

    public String generateDataUrl(Booking booking) {
        try {
            String content = String.format(
                    "SalonFlow|bookingId=%d|date=%s|time=%s|branch=%s",
                    booking.getId(),
                    booking.getBookingDate(),
                    booking.getStartTime(),
                    booking.getBranch() != null ? booking.getBranch().getName() : ""
            );

            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 220, 220);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            log.warn("Failed to generate QR code for booking {}: {}", booking != null ? booking.getId() : null, e.getMessage());
            return "";
        }
    }
}

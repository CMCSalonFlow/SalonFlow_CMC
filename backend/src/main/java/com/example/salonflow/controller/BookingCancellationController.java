package com.example.salonflow.controller;

import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.services.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingCancellationController {

    private final BookingService bookingService;

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<CancellationResult> cancelBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) String reason) {

        String decodedReason = reason;
        if (reason != null && !reason.trim().isEmpty()) {
            try {
                decodedReason = java.net.URLDecoder.decode(reason.trim(), java.nio.charset.StandardCharsets.UTF_8);
                if (decodedReason.endsWith("=")) {
                    decodedReason = decodedReason.substring(0, decodedReason.length() - 1);
                }
            } catch (Exception e) {
                decodedReason = reason;
            }
        }

        CancellationResult result = bookingService.cancelBooking(bookingId, decodedReason);
        return ResponseEntity.ok(result);
    }
}
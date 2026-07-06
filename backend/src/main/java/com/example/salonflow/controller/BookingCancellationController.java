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
        
        CancellationResult result = bookingService.cancelBooking(bookingId, reason);
        return ResponseEntity.ok(result);
    }
}
package com.example.salonflow.services.service;

import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.entity.Booking;

public interface EmailService {

    void sendVerificationOtp(
            String email,
            String otp
    );

    void sendResetPasswordEmail(
            String email,
            String resetLink
    );

    void sendCancellationEmail(Booking booking, CancellationResult result);
    void sendOverdueCancellationEmail(Booking booking);

    // Gửi email sau khi thanh toán thành công
    void sendInvoiceEmail(Booking booking, String invoiceUrl);
}

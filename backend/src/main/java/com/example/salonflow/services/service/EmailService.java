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

    void sendNotificationEmail(String to, String subject, String body);

    void sendSalonApprovedEmail(String toEmail, String salonName, String ownerName);

    void sendSalonRejectedEmail(String toEmail, String salonName, String ownerName, String reason);

    void sendWeeklyReportEmail(String toEmail, String ownerName, String salonName, java.util.Map<String, Object> reportData);

    void sendTicketCreatedEmail(String toEmail, String userName, String ticketCode, String subject, String priorityName, String slaDueStr);

    void sendTicketReplyNotificationEmail(String toEmail, String userName, String ticketCode, String senderName, String messageContent);

    void sendTicketStatusChangedEmail(String toEmail, String userName, String ticketCode, String newStatusName);
}

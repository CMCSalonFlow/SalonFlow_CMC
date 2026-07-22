package com.example.salonflow.services.service;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.User;

public interface ZaloZnsService {
    /**
     * Send ZNS notification for Booking Confirmation (Đặt lịch thành công).
     */
    boolean sendBookingCreatedZns(Booking booking, User customer);

    /**
     * Send ZNS notification for Appointment Reminder (Nhắc hẹn).
     */
    boolean sendAppointmentReminderZns(Booking booking, User customer);

    /**
     * Send ZNS notification for Booking Cancellation (Hủy lịch).
     */
    boolean sendBookingCancelledZns(Booking booking, User customer, String cancelReason);

    /**
     * Test sending ZNS to a custom phone number.
     */
    boolean sendTestZns(String phone, String templateId, String customerName);
}

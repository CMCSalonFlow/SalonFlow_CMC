package com.example.salonflow.entity.enums;

/**
 * Các trạng thái của một lịch hẹn đặt chỗ (Booking).
 */
public enum BookingStatus {
    PENDING,    // Chờ xác nhận từ cửa hàng / Đã lock slot chờ xác nhận
    CONFIRMED,  // Đã được xác nhận thành công
    CHECKED_IN, // Khách đã đến và check-in tại quầy
    COMPLETED,  // Đã hoàn thành buổi dịch vụ
    CANCELLED,  // Lịch hẹn đã bị hủy
    NO_SHOW     // Khách hàng không đến đúng giờ hẹn
}

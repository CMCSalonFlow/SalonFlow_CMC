package com.example.salonflow.entity.enums;

public enum BookingStatus {
    PENDING,    // Đã lock slot, chờ xác nhận
    CONFIRMED,  // Đã xác nhận
    CANCELLED,  // Đã hủy
    COMPLETED   // Hoàn thành
}

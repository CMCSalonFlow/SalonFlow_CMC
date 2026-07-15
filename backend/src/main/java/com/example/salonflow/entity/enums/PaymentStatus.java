package com.example.salonflow.entity.enums;

/**
 * Các trạng thái của một giao dịch thanh toán (Payment).
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED
}

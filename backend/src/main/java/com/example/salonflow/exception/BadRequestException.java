package com.example.salonflow.exception;

// File: BadRequestException.java
public class BadRequestException extends RuntimeException {
    
    // Constructor không tham số
    public BadRequestException() {
        super("Yêu cầu không hợp lệ (Bad Request).");
    }

    // Constructor nhận một thông báo lỗi cụ thể
    public BadRequestException(String message) {
        super(message);
    }

    // Constructor nhận thông báo lỗi và nguyên nhân (cause) nếu có
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
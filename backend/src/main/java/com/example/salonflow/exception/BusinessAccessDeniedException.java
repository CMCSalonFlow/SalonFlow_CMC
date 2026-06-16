package com.example.salonflow.exception;

public class BusinessAccessDeniedException extends RuntimeException {

    public BusinessAccessDeniedException(String message) {
        super(message);
    }
}

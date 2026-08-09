package com.example.salonflow.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ForecastException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ForecastException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

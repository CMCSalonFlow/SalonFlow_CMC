package com.example.salonflow.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity.badRequest()
                .body(errorBody(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        errors
                ));
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            RuntimeException ex
    ) {

        return ResponseEntity.badRequest()
                .body(errorBody(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage(),
                        null
                ));
    }
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleNotFound(
                ResourceNotFoundException ex
        ) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage(),
                        null
                ));
        }
        @ExceptionHandler(BusinessAccessDeniedException.class)
        public ResponseEntity<Map<String, Object>> handleAccessDenied(
                BusinessAccessDeniedException ex
        ) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(
                        HttpStatus.FORBIDDEN,
                        ex.getMessage(),
                        null
                ));
        }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity() {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(
                        HttpStatus.CONFLICT,
                        "Data already exists",
                        null
                ));
    }

    // @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(
            RuntimeException ex
    ) {

        return ResponseEntity.badRequest()
                .body(errorBody(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage(),
                        null
                ));
    }

    private Map<String, Object> errorBody(
            HttpStatus status,
            String message,
            Object details
    ) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        if (details != null) {
            body.put("details", details);
        }

        return body;
    }

        @ExceptionHandler({
                ResourceNotFoundException.class,
                InvalidTokenException.class,
                BusinessException.class
        })
        public ResponseEntity<Map<String, Object>> handleCustomException(
                RuntimeException ex
        ) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (ex instanceof ResourceNotFoundException) {
                status = HttpStatus.NOT_FOUND;
        }

        if (ex instanceof InvalidTokenException) {
                status = HttpStatus.UNAUTHORIZED;
        }

        return ResponseEntity.status(status)
                .body(errorBody(
                        status,
                        ex.getMessage(),
                        null
                ));
        }
}

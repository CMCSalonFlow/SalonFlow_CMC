package com.example.salonflow.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller - Kiểm tra CSRF Protection hoạt động
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class CsrfTestController {

    /**
     * GET endpoint - Không cần CSRF token
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> getPublic() {
        log.info("GET /test/public called");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Public GET request - không cần CSRF token");
        response.put("status", "success");
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST endpoint - Cần CSRF token
     * 
     * Để test endpoint này:
     * 1. GET /csrf-token để lấy token
     * 2. POST request với header X-CSRF-TOKEN
     */
    @PostMapping("/protected")
    public ResponseEntity<Map<String, String>> postProtected(
            @RequestBody Map<String, String> request,
            CsrfToken csrfToken) {
        
        log.info("POST /test/protected called with CSRF token");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Protected POST request - CSRF token verified!");
        response.put("status", "success");
        response.put("receivedData", request.toString());
        response.put("tokenVerified", "true");
        
        return ResponseEntity.ok(response);
    }

    /**
     * PUT endpoint - Cần CSRF token
     */
    @PutMapping("/protected/{id}")
    public ResponseEntity<Map<String, String>> putProtected(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            CsrfToken csrfToken) {
        
        log.info("PUT /test/protected/{} called with CSRF token", id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Protected PUT request - CSRF token verified!");
        response.put("status", "success");
        response.put("resourceId", id);
        response.put("updatedData", request.toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE endpoint - Cần CSRF token
     */
    @DeleteMapping("/protected/{id}")
    public ResponseEntity<Map<String, String>> deleteProtected(
            @PathVariable String id,
            CsrfToken csrfToken) {
        
        log.info("DELETE /test/protected/{} called with CSRF token", id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Protected DELETE request - CSRF token verified!");
        response.put("status", "success");
        response.put("deletedResourceId", id);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint test: Gọi POST mà không có CSRF token
     * Sẽ trả về 403 Forbidden
     */
    @PostMapping("/without-token")
    public ResponseEntity<Map<String, String>> postWithoutToken(
            @RequestBody Map<String, String> request) {
        
        // Endpoint này sẽ không bao giờ được gọi nếu CSRF protection hoạt động đúng
        Map<String, String> response = new HashMap<>();
        response.put("message", "Nếu bạn thấy thông báo này, CSRF protection không hoạt động!");
        response.put("status", "error");
        
        return ResponseEntity.ok(response);
    }
}

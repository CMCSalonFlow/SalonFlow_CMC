package com.example.salonflow.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor để thêm các security headers vào response
 */
@Slf4j
public class SecurityHeadersInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        
        // Ngăn chặn MIME-type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Ngăn chặn Clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // Kích hoạt XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Ngăn chặn Referrer leaking
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Content Security Policy (có thể cần điều chỉnh)
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self'; " +
            "connect-src 'self' http://localhost:5173; " +
            "frame-ancestors 'none'"
        );
        
        // Permissions Policy
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=()"
        );
        
        log.debug("Security headers added to response");
        
        return true;
    }
}

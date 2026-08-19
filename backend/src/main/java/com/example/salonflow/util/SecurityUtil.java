package com.example.salonflow.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static String getCurrentUsername() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return authentication.getName();
    }

    /**
     * Lấy IP thật của client, ưu tiên header X-Forwarded-For
     * (trường hợp app đứng sau reverse proxy / load balancer / Nginx).
     * Nếu không có, fallback về request.getRemoteAddr().
     *
     * Dùng cho việc ghi ip_address vào audit_logs (US-068).
     */
    public static String getClientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Header có thể chứa nhiều IP cách nhau bởi dấu phẩy
            // (client -> proxy1 -> proxy2 -> server), IP đầu tiên là client thật
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

}
package com.example.salonflow.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security Headers Filter
 * Tương đương helmet.js trong NestJS/Express
 *
 * Headers được thêm:
 * - X-Content-Type-Options       → chặn MIME sniffing
 * - X-Frame-Options               → chặn clickjacking
 * - Strict-Transport-Security     → bắt buộc HTTPS (HSTS)
 * - Content-Security-Policy       → chặn XSS, chỉ load resource tin cậy
 * - Referrer-Policy               → kiểm soát Referer header
 * - Permissions-Policy            → tắt browser API không cần thiết
 * - X-XSS-Protection              → bật XSS filter trình duyệt cũ
 * - Cache-Control                 → không cache response nhạy cảm
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain
    ) throws ServletException, IOException {

        // Chặn MIME type sniffing
        response.setHeader(
                "X-Content-Type-Options",
                "nosniff"
        );

        // Chặn clickjacking (không cho iframe nhúng app)
        response.setHeader(
                "X-Frame-Options",
                "DENY"
        );

        // Bắt buộc HTTPS trong 1 năm (bật khi production)
        response.setHeader(
                "Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload"
        );

        // Content Security Policy
        // Điều chỉnh nếu bạn load CDN, font, image từ domain khác
        response.setHeader(
                "Content-Security-Policy",
                String.join("; ",
                        "default-src 'self'",
                        "script-src 'self'",
                        "style-src 'self' 'unsafe-inline'",   // cho CSS-in-JS
                        "img-src 'self' data: https:",         // cho avatar từ OAuth2
                        "font-src 'self' https: data:",
                        "connect-src 'self'",
                        "object-src 'none'",
                        "frame-src 'none'",
                        "upgrade-insecure-requests"
                )
        );

        // Kiểm soát thông tin Referer header
        response.setHeader(
                "Referrer-Policy",
                "strict-origin-when-cross-origin"
        );

        // Tắt các browser API không cần thiết
        response.setHeader(
                "Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()"
        );

        // XSS Protection cho trình duyệt cũ
        response.setHeader(
                "X-XSS-Protection",
                "1; mode=block"
        );

        // Không cache các response nhạy cảm (auth, user data)
        String path = request.getRequestURI();
        if (isSensitivePath(path)) {
            response.setHeader(
                    "Cache-Control",
                    "no-store, no-cache, must-revalidate, private"
            );
            response.setHeader("Pragma", "no-cache");
        }

        // Ẩn thông tin server
        response.setHeader("Server", "");

        filterChain.doFilter(request, response);
    }

    private boolean isSensitivePath(String path) {
        return path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/users")
                || path.startsWith("/api/v1/roles");
    }
}

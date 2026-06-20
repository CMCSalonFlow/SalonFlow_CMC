package com.example.salonflow.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Filter (Redis-backed)
 *
 * Chiến lược: Sliding Window Counter per IP
 * - Global:      100 request / 60 giây
 * - Auth login:    5 request / 60 giây  (chống brute-force)
 * - Send OTP:      3 request / 60 giây  (chống spam OTP)
 * - Forgot pass:   3 request / 60 giây
 *
 * Key Redis: "rate:<bucket>:<ip>"
 * Value: số lần request trong window hiện tại
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    // ── Cấu hình giới hạn ────────────────────────────────────────
    private static final int  GLOBAL_LIMIT     = 100;
    private static final int  AUTH_LIMIT        = 5;
    private static final int  OTP_LIMIT         = 3;
    private static final long WINDOW_SECONDS    = 60L;

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain
    ) throws ServletException, IOException {

        String ip     = resolveClientIp(request);
        String path   = request.getRequestURI();
        String method = request.getMethod();

        // ── 1. Xác định bucket và limit ──────────────────────────
        String bucket;
        int    limit;

        if (isAuthLogin(path, method)) {
            bucket = "auth-login";
            limit  = AUTH_LIMIT;
        } else if (isSendOtp(path, method)) {
            bucket = "send-otp";
            limit  = OTP_LIMIT;
        } else if (isForgotPassword(path, method)) {
            bucket = "forgot-pass";
            limit  = OTP_LIMIT;
        } else {
            bucket = "global";
            limit  = GLOBAL_LIMIT;
        }

        // ── 2. Kiểm tra và tăng counter trong Redis ──────────────
        String  redisKey = "rate:" + bucket + ":" + ip;
        Long    count    = redisTemplate.opsForValue().increment(redisKey);

        // Lần đầu tiên → set TTL
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS));
        }

        // ── 3. Set header thông tin rate limit ───────────────────
        long remaining = (count == null) ? limit : Math.max(0, limit - count);
        response.setHeader("X-RateLimit-Limit",     String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Window",    WINDOW_SECONDS + "s");

        // ── 4. Vượt giới hạn → 429 Too Many Requests ────────────
        if (count != null && count > limit) {
            log.warn("[RateLimit] BLOCKED ip={} bucket={} count={}", ip, bucket, count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":429,"error":"Too Many Requests",\
                    "message":"Quá nhiều yêu cầu. Vui lòng thử lại sau %d giây."}
                    """.formatted(WINDOW_SECONDS));
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ── Helper: lấy IP thật khi đứng sau proxy/nginx ─────────────
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }

    // ── Helper: match endpoint ────────────────────────────────────
    private boolean isAuthLogin(String path, String method) {
        return "POST".equalsIgnoreCase(method)
                && path.endsWith("/api/v1/auth/login");
    }

    private boolean isSendOtp(String path, String method) {
        return "POST".equalsIgnoreCase(method)
                && path.endsWith("/api/v1/auth/send-otp");
    }

    private boolean isForgotPassword(String path, String method) {
        return "POST".equalsIgnoreCase(method)
                && path.endsWith("/api/v1/auth/forgot-password");
    }
}

package com.example.salonflow.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Rate Limiting Filter Unit Tests
 *
 * Test trực tiếp RateLimitFilter (không cần Spring Context, không cần
 * Redis thật, không cần Docker). Dùng MockHttpServletRequest/Response
 * + Mockito mock StringRedisTemplate.
 *
 * Chạy: mvn test -Dtest=RateLimitFilterTest
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RateLimitFilter filter;

    // Giả lập bộ nhớ Redis bằng Map trong JVM
    private final Map<String, AtomicLong> fakeRedisStore = new HashMap<>();

    private static int ipCounter = 1;

    private String uniqueIp() {
        return "10.0.0." + (ipCounter++ % 254 + 1);
    }

    @BeforeEach
    void setUp() {
        fakeRedisStore.clear();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Giả lập Redis increment & expire bằng Map
        lenient().when(valueOps.increment(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            AtomicLong val = fakeRedisStore.computeIfAbsent(key, k -> new AtomicLong(0));
            return val.incrementAndGet();
        });

        lenient().when(redisTemplate.expire(anyString(), any())).thenReturn(true);

        filter = new RateLimitFilter(redisTemplate);
    }

    // Helper giả lập request qua filter
    private MockHttpServletResponse runFilter(String method, String uri, String clientIp)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        request.setRemoteAddr(clientIp);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);
        return response;
    }

    // ═══════════════════════════════════════════════════════════
    // 1. GLOBAL RATE LIMIT (600 req/phút)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Request đầu tiên không bị rate limit")
    void globalLimit_firstRequest_shouldPassThrough() throws Exception {
        MockHttpServletResponse res = runFilter("GET", "/api/v1/roles", uniqueIp());

        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("✅ Response có header X-RateLimit-Limit = 600")
    void globalLimit_shouldHaveRateLimitHeaders() throws Exception {
        MockHttpServletResponse res = runFilter("GET", "/api/v1/roles", uniqueIp());

        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("600");
        assertThat(res.getHeader("X-RateLimit-Remaining")).isNotNull();
        assertThat(res.getHeader("X-RateLimit-Window")).isEqualTo("60s");
    }

    @Test
    @DisplayName("🚫 Gửi 601 request liên tiếp → request thứ 601 nhận 429")
    void globalLimit_601Requests_the601stReturns429() throws Exception {
        String ip = uniqueIp();
        int successCount = 0;
        int blockedCount = 0;
        MockHttpServletResponse lastResponse = null;

        for (int i = 1; i <= 601; i++) {
            lastResponse = runFilter("GET", "/api/v1/roles", ip);
            if (lastResponse.getStatus() == 429) {
                blockedCount++;
            } else {
                successCount++;
            }
        }

        assertThat(successCount).isEqualTo(600);
        assertThat(blockedCount).isEqualTo(1);
        assertThat(lastResponse.getStatus()).isEqualTo(429);
        assertThat(lastResponse.getContentAsString()).contains("Quá nhiều yêu cầu");
    }

    @Test
    @DisplayName("✅ Các IP khác nhau có counter độc lập")
    void globalLimit_differentIps_areIndependent() throws Exception {
        String ipA = uniqueIp();
        String ipB = uniqueIp();

        // Đẩy IP A vượt giới hạn (601 request)
        MockHttpServletResponse resA = null;
        for (int i = 0; i < 601; i++) {
            resA = runFilter("GET", "/api/v1/roles", ipA);
        }
        assertThat(resA.getStatus()).isEqualTo(429);

        // IP B hoàn toàn độc lập — vẫn được phép
        MockHttpServletResponse resB = runFilter("GET", "/api/v1/roles", ipB);
        assertThat(resB.getStatus()).isNotEqualTo(429);
    }

    // ═══════════════════════════════════════════════════════════
    // 2. AUTH LOGIN RATE LIMIT (15 req/phút)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Login 15 lần liên tiếp → không bị block")
    void authLimit_15Requests_allPassThrough() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 15; i++) {
            MockHttpServletResponse res =
                    runFilter("POST", "/api/v1/auth/login", ip);
            assertThat(res.getStatus()).isNotEqualTo(429);
        }
    }

    @Test
    @DisplayName("🚫 Login lần thứ 16 → 429 (auth limit = 15 req/phút)")
    void authLimit_16thRequest_shouldReturn429() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 15; i++) {
            runFilter("POST", "/api/v1/auth/login", ip);
        }

        MockHttpServletResponse res =
                runFilter("POST", "/api/v1/auth/login", ip);

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getContentAsString()).contains("Quá nhiều yêu cầu");
    }

    // ═══════════════════════════════════════════════════════════
    // 3. OTP / FORGOT-PASSWORD RATE LIMIT (10 req/phút)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("🚫 Send-OTP lần thứ 11 → 429 (otp limit = 10 req/phút)")
    void otpLimit_11thRequest_shouldReturn429() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 10; i++) {
            runFilter("POST", "/api/v1/auth/send-otp", ip);
        }

        MockHttpServletResponse res =
                runFilter("POST", "/api/v1/auth/send-otp", ip);

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("🚫 Forgot-password lần thứ 11 → 429")
    void forgotPasswordLimit_11thRequest_shouldReturn429() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 10; i++) {
            runFilter("POST", "/api/v1/auth/forgot-password", ip);
        }

        MockHttpServletResponse res =
                runFilter("POST", "/api/v1/auth/forgot-password", ip);

        assertThat(res.getStatus()).isEqualTo(429);
    }

    // ═══════════════════════════════════════════════════════════
    // 4. IP RESOLUTION
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Ưu tiên đọc IP từ header X-Forwarded-For nếu có NGINX proxy")
    void resolveIp_prefersXForwardedFor() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("GET");
        req.setRequestURI("/api/v1/roles");
        req.setRemoteAddr("10.0.0.1"); // IP ngầm định
        req.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18"); // Client IP thực

        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(req, res, filterChain);

        verify(redisTemplate).opsForValue();
        verify(valueOps).increment(contains("203.0.113.195"));
    }

    @Test
    @DisplayName("🚫 429 response block filter chain (không cho request đi tiếp)")
    void filterChain_isNotCalled_whenOverLimit() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 600; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setMethod("GET");
            req.setRequestURI("/api/v1/roles");
            req.setRemoteAddr(ip);
            FilterChain fc = mock(FilterChain.class);
            filter.doFilter(req, new MockHttpServletResponse(), fc);
        }

        MockHttpServletRequest reqOver = new MockHttpServletRequest();
        reqOver.setMethod("GET");
        reqOver.setRequestURI("/api/v1/roles");
        reqOver.setRemoteAddr(ip);

        MockHttpServletResponse resOver = new MockHttpServletResponse();
        FilterChain fcOver = mock(FilterChain.class);

        filter.doFilter(reqOver, resOver, fcOver);

        assertThat(resOver.getStatus()).isEqualTo(429);
        verify(fcOver, never()).doFilter(any(), any());
    }
}
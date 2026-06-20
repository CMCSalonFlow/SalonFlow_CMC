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
        filter = new RateLimitFilter(redisTemplate);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // increment(key) → tăng counter trong fakeRedisStore, trả về giá trị mới
        when(valueOps.increment(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return fakeRedisStore
                    .computeIfAbsent(key, k -> new AtomicLong(0))
                    .incrementAndGet();
        });

        // expire() — không cần làm gì, chỉ trả true
        when(redisTemplate.expire(anyString(), any())).thenReturn(true);
    }

    /** Helper: chạy filter 1 lần, trả về response để assert */
    private MockHttpServletResponse runFilter(
            String method, String uri, String ip
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("X-Forwarded-For", ip);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        return response;
    }

    // ═══════════════════════════════════════════════════════════
    // 1. GLOBAL RATE LIMIT (100 req/phút)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Request đầu tiên → cho phép đi qua (200, không bị set 429)")
    void globalLimit_firstRequest_shouldPassThrough() throws Exception {
        MockHttpServletResponse res = runFilter("GET", "/api/v1/roles", uniqueIp());

        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("✅ Response có header X-RateLimit-Limit = 100")
    void globalLimit_shouldHaveRateLimitHeaders() throws Exception {
        MockHttpServletResponse res = runFilter("GET", "/api/v1/roles", uniqueIp());

        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(res.getHeader("X-RateLimit-Remaining")).isNotNull();
        assertThat(res.getHeader("X-RateLimit-Window")).isEqualTo("60s");
    }

    @Test
    @DisplayName("🚫 Gửi 101 request liên tiếp → request thứ 101 nhận 429")
    void globalLimit_101Requests_the101stReturns429() throws Exception {
        String ip = uniqueIp();
        int successCount = 0;
        int blockedCount = 0;
        MockHttpServletResponse lastResponse = null;

        for (int i = 1; i <= 101; i++) {
            lastResponse = runFilter("GET", "/api/v1/roles", ip);
            if (lastResponse.getStatus() == 429) {
                blockedCount++;
            } else {
                successCount++;
            }
        }

        System.out.println("✓ Trong 101 requests: "
                + successCount + " thành công, " + blockedCount + " bị block");

        // 100 request đầu thành công, request 101 bị block
        assertThat(successCount).isEqualTo(100);
        assertThat(blockedCount).isEqualTo(1);
        assertThat(lastResponse.getStatus()).isEqualTo(429);
        assertThat(lastResponse.getContentAsString()).contains("Quá nhiều yêu cầu");
    }

    @Test
    @DisplayName("✅ Các IP khác nhau có counter độc lập")
    void globalLimit_differentIps_areIndependent() throws Exception {
        String ipA = uniqueIp();
        String ipB = uniqueIp();

        // Đẩy IP A vượt giới hạn (101 request)
        MockHttpServletResponse resA = null;
        for (int i = 0; i < 101; i++) {
            resA = runFilter("GET", "/api/v1/roles", ipA);
        }
        assertThat(resA.getStatus()).isEqualTo(429);

        // IP B hoàn toàn độc lập — vẫn được phép
        MockHttpServletResponse resB = runFilter("GET", "/api/v1/roles", ipB);
        assertThat(resB.getStatus()).isNotEqualTo(429);
    }

    // ═══════════════════════════════════════════════════════════
    // 2. AUTH LOGIN RATE LIMIT (5 req/phút)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Login 5 lần liên tiếp → không bị block")
    void authLimit_5Requests_allPassThrough() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res =
                    runFilter("POST", "/api/v1/auth/login", ip);
            assertThat(res.getStatus()).isNotEqualTo(429);
        }
    }

    @Test
    @DisplayName("🚫 Login lần thứ 6 → 429 (auth limit = 5 req/phút)")
    void authLimit_6thRequest_shouldReturn429() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 5; i++) {
            runFilter("POST", "/api/v1/auth/login", ip);
        }

        MockHttpServletResponse res =
                runFilter("POST", "/api/v1/auth/login", ip);

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getContentAsString()).contains("Quá nhiều yêu cầu");
    }

    // ═══════════════════════════════════════════════════════════
    // 3. OTP / FORGOT-PASSWORD RATE LIMIT (3 req/phút)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("🚫 Send-OTP lần thứ 4 → 429 (otp limit = 3 req/phút)")
    void otpLimit_4thRequest_shouldReturn429() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 3; i++) {
            runFilter("POST", "/api/v1/auth/send-otp", ip);
        }

        MockHttpServletResponse res =
                runFilter("POST", "/api/v1/auth/send-otp", ip);

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("🚫 Forgot-password lần thứ 4 → 429")
    void forgotPasswordLimit_4thRequest_shouldReturn429() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 3; i++) {
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
    @DisplayName("✅ X-Real-IP được dùng khi không có X-Forwarded-For")
    void resolveIp_usesXRealIpAsFallback() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/roles");
        request.addHeader("X-Real-IP", uniqueIp());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isNotEqualTo(429);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("✅ FilterChain.doFilter() được gọi khi không vượt limit")
    void filterChain_isCalled_whenUnderLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/roles");
        request.addHeader("X-Forwarded-For", uniqueIp());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("🚫 FilterChain.doFilter() KHÔNG được gọi khi vượt limit")
    void filterChain_isNotCalled_whenOverLimit() throws Exception {
        String ip = uniqueIp();

        // Đẩy vượt giới hạn auth (5 req)
        for (int i = 0; i < 5; i++) {
            runFilter("POST", "/api/v1/auth/login", ip);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("X-Forwarded-For", ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(any(), any());
    }
}
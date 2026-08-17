package com.example.salonflow.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@RequiredArgsConstructor
public class MonitoringPerformanceFilter extends OncePerRequestFilter {

    private final MonitoringAlertService monitoringAlertService;
    private final MeterRegistry meterRegistry;

    @Value("${app.monitoring.alert.latency-threshold-ms:2000}")
    private long latencyThresholdMs;

    @Value("${app.monitoring.alert.error-rate-threshold-percent:1.0}")
    private double errorRateThresholdPercent;

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong errorRequests = new AtomicLong(0);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Bỏ qua các static resources hoặc polling nếu cần
        if (uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        totalRequests.incrementAndGet();

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            errorRequests.incrementAndGet();
            long duration = System.currentTimeMillis() - startTime;
            monitoringAlertService.alertServerError(method, uri, 500, ex.getMessage(), ex);
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // Record Micrometer metric
            Timer.builder("salonflow.http.request.duration")
                    .tag("method", method)
                    .tag("status", String.valueOf(status))
                    .tag("uri", sanitizeUri(uri))
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);

            // Cảnh báo 1: Latency > 2s (hoặc cấu hình)
            if (duration > latencyThresholdMs) {
                monitoringAlertService.alertSlowRequest(method, uri, duration, status);
            }

            // Cảnh báo 2: Lỗi Server 5xx
            if (status >= 500) {
                errorRequests.incrementAndGet();
                monitoringAlertService.alertServerError(method, uri, status, "HTTP " + status + " Internal Server Error", null);
            }

            // Cảnh báo 3: Kiểm tra tỷ lệ lỗi nếu đã có ít nhất 100 requests
            long total = totalRequests.get();
            if (total >= 100) {
                double currentErrorRate = (errorRequests.get() * 100.0) / total;
                if (currentErrorRate > errorRateThresholdPercent) {
                    monitoringAlertService.sendSlackAlert(
                            "⚠️ [CẢNH BÁO] Tỷ lệ lỗi API vượt ngưỡng > 1%",
                            String.format("Tỷ lệ lỗi hiện tại: **%.2f%%** (Tổng %d requests, %d lỗi). Ngưỡng cho phép: %.2f%%",
                                    currentErrorRate, total, errorRequests.get(), errorRateThresholdPercent),
                            "#E91E63",
                            java.util.Map.of(
                                    "Error Rate", String.format("%.2f%%", currentErrorRate),
                                    "Total Requests", String.valueOf(total),
                                    "Failed Requests", String.valueOf(errorRequests.get())
                            )
                    );
                }
            }
        }
    }

    private String sanitizeUri(String uri) {
        // Gom nhóm ID số thành /{id} để tránh cardinality nổ tung trong Prometheus
        return uri.replaceAll("/\\d+", "/{id}");
    }
}

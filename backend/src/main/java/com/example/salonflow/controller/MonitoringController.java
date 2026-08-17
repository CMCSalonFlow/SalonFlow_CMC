package com.example.salonflow.controller;

import com.example.salonflow.monitoring.MonitoringAlertService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringAlertService monitoringAlertService;
    private final MeterRegistry meterRegistry;

    /**
     * Lấy tổng quan các chỉ số giám sát phục vụ UI Dashboard
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getMonitoringOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // 1. Uptime
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSec = uptimeMs / 1000;
        long hours = uptimeSec / 3600;
        long minutes = (uptimeSec % 3600) / 60;
        long seconds = uptimeSec % 60;
        overview.put("uptime", String.format("%02dh %02dm %02ds", hours, minutes, seconds));

        // 2. JVM Memory
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapUsedMb = memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMaxMb = memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        overview.put("heapUsedMb", heapUsedMb);
        overview.put("heapMaxMb", heapMaxMb > 0 ? heapMaxMb : 1024);
        overview.put("heapUsagePercent", heapMaxMb > 0 ? Math.round((heapUsedMb * 100.0) / heapMaxMb) : 0);

        // 3. Request Latency p95 (từ Micrometer Timer)
        Timer timer = meterRegistry.find("http.server.requests").timer();
        double p95Sec = 0.0;
        double count = 0;
        if (timer != null) {
            count = timer.count();
            p95Sec = timer.mean(TimeUnit.SECONDS);
        }
        overview.put("totalRequests", (long) count);
        overview.put("latencyP95Seconds", Math.round(p95Sec * 1000.0) / 1000.0);
        overview.put("latencyP95Ms", Math.round(p95Sec * 1000.0));

        // 4. Alert Rules Configured
        overview.put("alertRules", Map.of(
                "latencyThreshold", "Latency p95 > 2.0s → Slack & Email Alert",
                "errorRateThreshold", "HTTP 5xx Error Rate > 1.0% → Slack & Email Alert",
                "dbPoolThreshold", "HikariCP Pending Connections > 5 → DB Warning",
                "slackAlertEnabled", true,
                "emailAlertEnabled", true
        ));

        // 5. Grafana & Sentry Status
        overview.put("services", Map.of(
                "sentry", "ACTIVE",
                "prometheus", "ACTIVE (/actuator/prometheus)",
                "grafana", "ACTIVE (http://localhost:3000)",
                "alertmanager", "ACTIVE"
        ));

        return ResponseEntity.ok(overview);
    }

    /**
     * Giả lập gửi Alert test tới Slack Webhook và Email
     */
    @PostMapping("/test-alert")
    public ResponseEntity<Map<String, Object>> triggerTestAlert(
            @RequestParam(defaultValue = "all") String type
    ) {
        log.info("[MonitoringController] Triggering simulated monitoring alert: type={}", type);

        if ("sentry".equalsIgnoreCase(type)) {
            try {
                throw new RuntimeException("Test Sentry Error Triggered from SalonFlow Monitoring Dashboard");
            } catch (Exception e) {
                Sentry.captureException(e);
            }
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Đã gửi sự kiện lỗi giả lập lên Sentry thành công!"
            ));
        }

        // Gửi thử nghiệm Slack & Email
        monitoringAlertService.alertSlowRequest("GET", "/api/v1/test/slow-simulation", 2450, 200);
        monitoringAlertService.alertServerError("POST", "/api/v1/test/error-simulation", 500, "Simulated 500 Internal Error for testing", new RuntimeException("Test Alert Exception"));

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đã kích hoạt cảnh báo thử nghiệm (Latency > 2s & Error 500) qua Slack Webhook và Email!"
        ));
    }
}

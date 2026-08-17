package com.example.salonflow.monitoring;

import com.example.salonflow.services.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringAlertService {

    private final EmailService emailService;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.monitoring.slack.webhook-url:}")
    private String slackWebhookUrl;

    @Value("${app.monitoring.slack.enabled:true}")
    private boolean slackEnabled;

    @Value("${app.monitoring.alert.email-to:admin@salonflow.vn}")
    private String alertEmailTo;

    // Rate limiter để tránh spam alert: key -> timestamp (ms)
    private final ConcurrentHashMap<String, Long> lastAlertTimeMap = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 60_000L; // 1 phút cooldown cho mỗi loại alert

    /**
     * Gửi cảnh báo khi độ trễ yêu cầu vượt ngưỡng (Latency > 2s)
     */
    @Async
    public void alertSlowRequest(String method, String uri, long durationMs, int statusCode) {
        String alertKey = "SLOW_REQUEST:" + method + ":" + uri;
        if (isThrottled(alertKey)) {
            return;
        }

        String title = "🚨 [CẢNH BÁO] Độ trễ API vượt ngưỡng > 2s (Latency Alert)";
        String message = String.format(
                "Yêu cầu `%s %s` mất **%d ms** (ngưỡng cho phép: 2000 ms) - Status: `%d`",
                method, uri, durationMs, statusCode
        );

        log.warn("[MonitoringAlert] Slow request detected: {} {} in {} ms", method, uri, durationMs);
        sendSlackAlert(title, message, "#FF9800", Map.of(
                "Endpoint", method + " " + uri,
                "Latency", durationMs + " ms",
                "HTTP Status", String.valueOf(statusCode),
                "Thời gian", getCurrentTimeStr()
        ));

        sendEmailAlert(title, buildAlertEmailHtml(title, message, Map.of(
                "Endpoint", method + " " + uri,
                "Latency", durationMs + " ms",
                "HTTP Status", String.valueOf(statusCode)
        )));
    }

    /**
     * Gửi cảnh báo khi tỷ lệ lỗi hoặc lỗi nghiêm trọng 5xx xuất hiện
     */
    @Async
    public void alertServerError(String method, String uri, int statusCode, String errorMessage, Throwable throwable) {
        String alertKey = "SERVER_ERROR:" + method + ":" + uri;
        if (isThrottled(alertKey)) {
            return;
        }

        String title = "🔥 [CẢNH BÁO KHẨN] Lỗi hệ thống Backend (Error Rate / 5xx Alert)";
        String exceptionDetail = throwable != null ? throwable.getClass().getSimpleName() + ": " + throwable.getMessage() : errorMessage;
        String message = String.format(
                "Endpoint `%s %s` trả về mã lỗi `%d`.\n**Chi tiết:** %s",
                method, uri, statusCode, exceptionDetail
        );

        log.error("[MonitoringAlert] Server error alert triggered: {} {} status {}", method, uri, statusCode);
        sendSlackAlert(title, message, "#F44336", Map.of(
                "Endpoint", method + " " + uri,
                "HTTP Status", String.valueOf(statusCode),
                "Lỗi", exceptionDetail != null ? exceptionDetail : "Internal Server Error",
                "Thời gian", getCurrentTimeStr()
        ));

        sendEmailAlert(title, buildAlertEmailHtml(title, message, Map.of(
                "Endpoint", method + " " + uri,
                "HTTP Status", String.valueOf(statusCode),
                "Lỗi chi tiết", exceptionDetail != null ? exceptionDetail : "N/A"
        )));
    }

    /**
     * Gửi tin nhắn Card định dạng Slack Block Kit
     */
    public void sendSlackAlert(String title, String summary, String colorHex, Map<String, String> fields) {
        if (!slackEnabled || slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
            log.debug("[MonitoringAlert] Slack webhook is not configured or disabled. Skipping Slack alert.");
            return;
        }

        try {
            List<Map<String, Object>> fieldsList = fields.entrySet().stream()
                    .map(e -> Map.<String, Object>of(
                            "title", e.getKey(),
                            "value", e.getValue(),
                            "short", true
                    ))
                    .toList();

            Map<String, Object> attachment = Map.of(
                    "color", colorHex,
                    "title", title,
                    "text", summary,
                    "fields", fieldsList,
                    "footer", "SalonFlow Monitoring & Alerting System",
                    "ts", System.currentTimeMillis() / 1000
            );

            Map<String, Object> payload = Map.of("attachments", List.of(attachment));

            webClientBuilder.build()
                    .post()
                    .uri(slackWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            res -> log.info("[MonitoringAlert] Slack alert sent successfully"),
                            err -> log.error("[MonitoringAlert] Failed to send Slack alert: {}", err.getMessage())
                    );
        } catch (Exception e) {
            log.error("[MonitoringAlert] Error preparing Slack alert payload: {}", e.getMessage());
        }
    }

    /**
     * Gửi Email thông báo sự cố qua EmailService
     */
    public void sendEmailAlert(String subject, String htmlBody) {
        if (alertEmailTo == null || alertEmailTo.isBlank()) {
            return;
        }
        try {
            emailService.sendNotificationEmail(alertEmailTo, subject, htmlBody);
            log.info("[MonitoringAlert] Email alert dispatched to {}", alertEmailTo);
        } catch (Exception e) {
            log.error("[MonitoringAlert] Failed to dispatch email alert: {}", e.getMessage());
        }
    }

    private boolean isThrottled(String key) {
        long now = System.currentTimeMillis();
        Long lastTime = lastAlertTimeMap.get(key);
        if (lastTime != null && (now - lastTime) < COOLDOWN_MS) {
            return true;
        }
        lastAlertTimeMap.put(key, now);
        return false;
    }

    private String getCurrentTimeStr() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String buildAlertEmailHtml(String title, String summary, Map<String, String> fields) {
        StringBuilder rows = new StringBuilder();
        fields.forEach((k, v) -> rows.append(String.format(
                "<tr><td style='padding:8px;font-weight:bold;border-bottom:1px solid #eee;'>%s</td><td style='padding:8px;border-bottom:1px solid #eee;'>%s</td></tr>",
                k, v
        )));

        return """
                <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;max-width:600px;margin:0 auto;border:1px solid #e0e0e0;border-radius:8px;overflow:hidden;">
                  <div style="background-color:#d32f2f;color:#fff;padding:16px 20px;">
                    <h2 style="margin:0;font-size:18px;">%s</h2>
                  </div>
                  <div style="padding:20px;background-color:#ffffff;">
                    <p style="font-size:15px;color:#333;line-height:1.5;">%s</p>
                    <table style="width:100%%;border-collapse:collapse;margin-top:16px;font-size:14px;color:#444;">
                      %s
                    </table>
                  </div>
                  <div style="background-color:#f5f5f5;padding:12px 20px;font-size:12px;color:#888;text-align:center;">
                    Hệ thống cảnh báo tự động SalonFlow Monitoring & Alerting
                  </div>
                </div>
                """.formatted(title, summary, rows.toString());
    }
}

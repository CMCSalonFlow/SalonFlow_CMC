package com.example.salonflow.scheduler;

import com.example.salonflow.services.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryScheduler {

    private final SubscriptionService subscriptionService;

    // Chạy vào lúc 00:00 hàng ngày
    @Scheduled(cron = "0 0 0 * * *")
    public void checkExpiredSubscriptions() {
        log.info("Bắt đầu quét kiểm tra các gói đăng ký hết hạn...");
        try {
            subscriptionService.checkExpiry();
            log.info("Hoàn thành quét kiểm tra các gói đăng ký hết hạn.");
        } catch (Exception e) {
            log.error("Lỗi khi quét kiểm tra các gói đăng ký hết hạn: ", e);
        }
    }
}

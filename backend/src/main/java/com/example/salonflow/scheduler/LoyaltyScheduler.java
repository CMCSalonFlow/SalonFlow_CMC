package com.example.salonflow.scheduler;

import com.example.salonflow.services.service.LoyaltyPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoyaltyScheduler {

    private final LoyaltyPointService loyaltyPointService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void processExpiredLoyaltyPoints() {
        log.info("Starting daily job to expire loyalty points...");
        try {
            loyaltyPointService.expirePointsJob();
            log.info("Completed daily job for expired loyalty points.");
        } catch (Exception e) {
            log.error("Error processing expired loyalty points job", e);
        }
    }
}

package com.example.salonflow.scheduler;

import com.example.salonflow.services.service.RevenueForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RevenueForecastScheduler {

    private final RevenueForecastService revenueForecastService;

    @Value("${app.forecast.retrain-months:2}")
    private Integer retrainMonths;

    @Scheduled(cron = "${app.forecast.retrain-cron:0 0 2 1 * *}", zone = "Asia/Bangkok")
    public void retrainMonthlyRevenueForecasts() {
        log.info("Starting monthly revenue forecast retrain job");
        revenueForecastService.trainAllBranchRevenueModels(retrainMonths);
        log.info("Completed monthly revenue forecast retrain job");
    }
}

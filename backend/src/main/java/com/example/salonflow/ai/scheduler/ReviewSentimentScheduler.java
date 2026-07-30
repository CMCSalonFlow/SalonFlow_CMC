package com.example.salonflow.ai.scheduler;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.service.ReviewSentimentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSentimentScheduler {

    private final ReviewSentimentService reviewSentimentService;
    private final AiProperties aiProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        trigger();
    }

    @Scheduled(fixedDelayString = "${app.ai.review.scan-interval-ms:1800000}")
    public void scheduledTrigger() {
        trigger();
    }

    private void trigger() {
        if (!aiProperties.isEnabled() || !aiProperties.getReview().isEnabled()) {
            return;
        }
        log.debug("Triggering review sentiment async job");
        reviewSentimentService.enqueuePendingReviews();
    }
}


package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.dto.sentiment.ReviewSentimentAnalysisRequest;
import com.example.salonflow.ai.dto.sentiment.ReviewSentimentAnalysisResult;
import com.example.salonflow.ai.dto.sentiment.ReviewSentimentLabel;
import com.example.salonflow.ai.provider.ReviewSentimentProvider;
import com.example.salonflow.ai.provider.ReviewSentimentProviderRouter;
import com.example.salonflow.ai.service.ReviewSentimentService;
import com.example.salonflow.entity.Review;
import com.example.salonflow.entity.enums.ReviewSentiment;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import com.example.salonflow.repository.ReviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSentimentServiceImpl implements ReviewSentimentService {

    private final ReviewRepository reviewRepository;
    private final ReviewSentimentProviderRouter providerRouter;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    @Async("aiTaskExecutor")
    public void enqueuePendingReviews() {
        if (!aiProperties.isEnabled() || !aiProperties.getReview().isEnabled()) {
            log.debug("Review sentiment job skipped because AI review processing is disabled");
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.debug("Review sentiment job is already running");
            return;
        }

        try {
            processPendingReviews();
        } finally {
            running.set(false);
        }
    }

    @Override
    @Transactional
    public void processReview(Long reviewId) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            return;
        }
        Review review = reviewOpt.get();
        analyzeAndPersist(review);
    }

    @Transactional
    protected void processPendingReviews() {
        int batchSize = Math.max(1, aiProperties.getReview().getBatchSize());
        int totalProcessed = 0;

        while (true) {
            var page = reviewRepository.findBySentimentStatusOrderByCreatedAtAsc(
                    ReviewSentimentStatus.PENDING,
                    PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "createdAt"))
            );

            if (page.isEmpty()) {
                if (totalProcessed == 0) {
                    log.info("No pending reviews found for sentiment analysis");
                } else {
                    log.info("Sentiment analysis finished, processed {} reviews", totalProcessed);
                }
                return;
            }

            for (Review review : page.getContent()) {
                analyzeAndPersist(review);
                totalProcessed++;
            }
        }
    }

    private void analyzeAndPersist(Review review) {
        if (review == null || review.getContent() == null || review.getContent().isBlank()) {
            markFailed(review, "Review content is empty");
            return;
        }

        try {
            review.setSentimentStatus(ReviewSentimentStatus.PROCESSING);
            reviewRepository.save(review);

            ReviewSentimentProvider provider = providerRouter.resolveProvider();
            ReviewSentimentAnalysisResult result = provider.analyze(
                    new ReviewSentimentAnalysisRequest(review.getId(), review.getContent())
            );

            review.setSentiment(mapLabel(result.sentiment()));
            review.setSentimentConfidence(normalizeConfidence(result.confidence()));
            review.setSentimentProvider(result.provider());
            review.setSentimentStatus(ReviewSentimentStatus.COMPLETED);
            review.setSentimentAnalyzedAt(Instant.now());
            review.setSentimentError(null);
            reviewRepository.save(review);

            log.info("Review {} analyzed with sentiment={} confidence={} provider={}",
                    review.getId(),
                    review.getSentiment(),
                    review.getSentimentConfidence(),
                    review.getSentimentProvider());
        } catch (Exception ex) {
            markFailed(review, ex.getMessage());
            log.warn("Failed to analyze review {}: {}", review.getId(), ex.getMessage());
        }
    }

    private void markFailed(Review review, String error) {
        if (review == null) {
            return;
        }

        review.setSentimentStatus(ReviewSentimentStatus.FAILED);
        review.setSentimentError(error);
        review.setSentimentAnalyzedAt(Instant.now());
        reviewRepository.save(review);
    }

    private ReviewSentiment mapLabel(ReviewSentimentLabel label) {
        if (label == null) {
            return ReviewSentiment.NEUTRAL;
        }
        return switch (label) {
            case POSITIVE -> ReviewSentiment.POSITIVE;
            case NEGATIVE -> ReviewSentiment.NEGATIVE;
            case NEUTRAL -> ReviewSentiment.NEUTRAL;
        };
    }

    private BigDecimal normalizeConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return BigDecimal.valueOf(0.50).setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal normalized = confidence;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            normalized = BigDecimal.ZERO;
        } else if (normalized.compareTo(BigDecimal.ONE) > 0) {
            normalized = BigDecimal.ONE;
        }

        if (normalized.compareTo(BigDecimal.valueOf(aiProperties.getReview().getLowConfidenceThreshold())) < 0) {
            return BigDecimal.valueOf(aiProperties.getReview().getLowConfidenceThreshold())
                    .setScale(4, RoundingMode.HALF_UP);
        }

        return normalized.setScale(4, RoundingMode.HALF_UP);
    }
}


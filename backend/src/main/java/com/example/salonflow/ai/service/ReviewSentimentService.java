package com.example.salonflow.ai.service;

public interface ReviewSentimentService {

    void enqueuePendingReviews();

    void processReview(Long reviewId);
}


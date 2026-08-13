package com.example.salonflow.services.service;

import com.example.salonflow.dto.subscription.ManualSubscriptionRequest;
import com.example.salonflow.dto.subscription.StripeCheckoutRequest;
import com.example.salonflow.dto.subscription.SubscriptionResponse;
import com.example.salonflow.entity.SubscriptionFeatures;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse getActiveSubscription(Long salonId);

    SubscriptionFeatures getActiveFeatures(Long salonId);

    List<SubscriptionResponse> getSubscriptionHistory(Long salonId);

    String createStripeCheckoutSession(Long salonId, StripeCheckoutRequest request);

    String createStripePortalSession(Long salonId, String returnUrl);

    void handleStripeWebhook(String payload, String sigHeader);

    SubscriptionResponse createManualSubscription(ManualSubscriptionRequest request);

    void checkExpiry();

    void validateBranchLimit(Long salonId);

    void validateStaffLimit(Long salonId);

    void validateAdvancedAnalytics(Long salonId);

    void validateAiFeatures(Long salonId);
}

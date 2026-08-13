package com.example.salonflow.controller;

import com.example.salonflow.dto.Salon.SalonResponse;
import com.example.salonflow.dto.subscription.ManualSubscriptionRequest;
import com.example.salonflow.dto.subscription.StripeCheckoutRequest;
import com.example.salonflow.dto.subscription.SubscriptionResponse;
import com.example.salonflow.services.service.SalonService;
import com.example.salonflow.services.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SalonService salonService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<SubscriptionResponse> getActiveSubscription() {
        SalonResponse mySalon = salonService.getMine();
        return ResponseEntity.ok(subscriptionService.getActiveSubscription(mySalon.getId()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('SALON_OWNER')")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionHistory() {
        SalonResponse mySalon = salonService.getMine();
        return ResponseEntity.ok(subscriptionService.getSubscriptionHistory(mySalon.getId()));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('SALON_OWNER')")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @Valid @RequestBody StripeCheckoutRequest request
    ) {
        SalonResponse mySalon = salonService.getMine();
        String checkoutUrl = subscriptionService.createStripeCheckoutSession(mySalon.getId(), request);
        return ResponseEntity.ok(Map.of("url", checkoutUrl));
    }

    @PostMapping("/portal")
    @PreAuthorize("hasRole('SALON_OWNER')")
    public ResponseEntity<Map<String, String>> createPortalSession(
            @RequestParam("returnUrl") String returnUrl
    ) {
        SalonResponse mySalon = salonService.getMine();
        String portalUrl = subscriptionService.createStripePortalSession(mySalon.getId(), returnUrl);
        return ResponseEntity.ok(Map.of("url", portalUrl));
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        subscriptionService.handleStripeWebhook(payload, sigHeader);
    }

    @PostMapping("/admin/manual")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionResponse> createManualSubscription(
            @Valid @RequestBody ManualSubscriptionRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.createManualSubscription(request));
    }
}

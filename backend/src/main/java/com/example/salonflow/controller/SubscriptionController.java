package com.example.salonflow.controller;

import com.example.salonflow.dto.Salon.SalonResponse;
import com.example.salonflow.dto.subscription.ManualSubscriptionRequest;
import com.example.salonflow.dto.subscription.StripeCheckoutRequest;
import com.example.salonflow.dto.subscription.SubscriptionResponse;
import com.example.salonflow.dto.subscription.UpdateSubscriptionRequest;
import com.example.salonflow.entity.enums.SubscriptionPlan;
import com.example.salonflow.entity.enums.SubscriptionStatus;
import com.example.salonflow.services.service.SalonService;
import com.example.salonflow.services.service.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<SubscriptionResponse>> getAllSubscriptions(
            @RequestParam(value = "salonId", required = false) Long salonId,
            @RequestParam(value = "plan", required = false) SubscriptionPlan plan,
            @RequestParam(value = "status", required = false) SubscriptionStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        String[] sortParams = sort.split(",");
        Sort sorting = Sort.by(
                sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ? 
                        Sort.Direction.ASC : 
                        Sort.Direction.DESC, 
                sortParams[0]
        );
        Pageable pageable = PageRequest.of(page, size, sorting);
        return ResponseEntity.ok(subscriptionService.getAllSubscriptionsForAdmin(salonId, plan, status, pageable));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateSubscriptionRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.updateSubscriptionForAdmin(id, request));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> cancelSubscription(
            @PathVariable("id") Long id
    ) {
        subscriptionService.cancelSubscriptionForAdmin(id);
        return ResponseEntity.noContent().build();
    }
}

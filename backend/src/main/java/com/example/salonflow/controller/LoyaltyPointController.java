package com.example.salonflow.controller;

import com.example.salonflow.dto.loyalty.LoyaltySummaryResponse;
import com.example.salonflow.dto.loyalty.LoyaltyTransactionResponse;
import com.example.salonflow.dto.loyalty.RedeemPointsRequest;
import com.example.salonflow.dto.loyalty.RedeemPointsResponse;
import com.example.salonflow.security.CustomUserPrincipal;
import com.example.salonflow.services.service.LoyaltyPointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyPointController {

    private final LoyaltyPointService loyaltyPointService;

    @GetMapping("/summary")
    public ResponseEntity<LoyaltySummaryResponse> getSummary(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(loyaltyPointService.getUserLoyaltySummary(principal.getId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<LoyaltyTransactionResponse>> getHistory(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(loyaltyPointService.getUserTransactionHistory(principal.getId()));
    }

    @PostMapping("/redeem")
    public ResponseEntity<RedeemPointsResponse> redeemPoints(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody RedeemPointsRequest request
    ) {
        return ResponseEntity.ok(loyaltyPointService.redeemPointsForVoucher(principal.getId(), request));
    }
}

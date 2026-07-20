package com.example.salonflow.services.service;

import com.example.salonflow.dto.loyalty.LoyaltySummaryResponse;
import com.example.salonflow.dto.loyalty.LoyaltyTransactionResponse;
import com.example.salonflow.dto.loyalty.RedeemPointsRequest;
import com.example.salonflow.dto.loyalty.RedeemPointsResponse;

import java.math.BigDecimal;
import java.util.List;

public interface LoyaltyPointService {
    void earnPointsForBooking(Long userId, BigDecimal orderTotal, String bookingReferenceId);
    LoyaltySummaryResponse getUserLoyaltySummary(Long userId);
    List<LoyaltyTransactionResponse> getUserTransactionHistory(Long userId);
    RedeemPointsResponse redeemPointsForVoucher(Long userId, RedeemPointsRequest request);
    void expirePointsJob();
}

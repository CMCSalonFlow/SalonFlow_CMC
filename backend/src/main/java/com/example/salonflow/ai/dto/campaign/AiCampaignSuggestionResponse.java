package com.example.salonflow.ai.dto.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCampaignSuggestionResponse {
    private String segmentType;
    private String campaignName;
    private String suggestedTitle;
    private String suggestedMessage;
    
    // Gợi ý thông số Voucher
    private String discountType; // 'PERCENTAGE' hoặc 'FIXED'
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    
    private String strategyExplanation; // Giải thích chiến lược tiếp thị của AI
}

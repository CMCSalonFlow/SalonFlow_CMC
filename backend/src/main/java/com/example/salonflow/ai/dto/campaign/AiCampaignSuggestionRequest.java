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
public class AiCampaignSuggestionRequest {
    private String segmentType; // 'NEW', 'RETURNING', 'VIP', 'AT_RISK'
    private Long branchId;
    private String goalDescription; // Mô tả mục tiêu / chủ đề tùy chỉnh
    private String discountType; // 'PERCENTAGE' or 'FIXED'
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
}

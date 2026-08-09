package com.example.salonflow.ai.dto.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCampaignSuggestionRequest {
    private String segmentType; // 'NEW', 'RETURNING', 'VIP', 'AT_RISK'
    private Long branchId;
    private String goalDescription; // Mô tả mục tiêu tùy chọn từ người dùng
}

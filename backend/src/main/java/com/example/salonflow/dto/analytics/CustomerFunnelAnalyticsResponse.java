package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFunnelAnalyticsResponse {
    private Long salonId;
    private Long branchId;
    private List<FunnelStageDto> stages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunnelStageDto {
        private String stageKey;     // 'TOTAL_INTERACTED', 'NEW_CUSTOMER', 'RETURNING_CUSTOMER', 'VIP_CUSTOMER'
        private String stageName;    // Tên hiển thị tiếng Việt
        private Long count;          // Số lượng khách hàng ở giai đoạn này
        private Double conversionRate; // Tỷ lệ chuyển đổi so với giai đoạn trước (%)
        private Double overallRate;    // Tỷ lệ so với tổng số lượng khách (%)
    }
}

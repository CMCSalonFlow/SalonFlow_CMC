package com.example.salonflow.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetedCampaignCreateRequest {
    @NotBlank(message = "Tên chiến dịch không được để trống")
    private String campaignName;

    @NotBlank(message = "Segment không được để trống")
    private String segmentType; // 'NEW', 'RETURNING', 'VIP', 'AT_RISK'

    private Long branchId;

    @NotBlank(message = "Tiêu đề thông báo không được để trống")
    private String messageTitle;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String messageContent;

    // Tùy chọn đính kèm tạo voucher mới phát hành cho segment này
    private Boolean createVoucher;
    private String voucherCode;
    private String discountType; // 'PERCENTAGE' or 'FIXED'
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer validDays; // Số ngày hiệu lực của voucher (ví dụ 14 ngày)
}

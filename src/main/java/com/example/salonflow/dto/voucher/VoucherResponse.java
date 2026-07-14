package com.example.salonflow.dto.voucher;

import com.example.salonflow.entity.Voucher;
import com.example.salonflow.entity.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VoucherResponse {

    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static VoucherResponse from(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .maxUses(v.getMaxUses())
                .usedCount(v.getUsedCount())
                .expiresAt(v.getExpiresAt())
                .isActive(v.getIsActive())
                .createdAt(v.getCreatedAt())
                .build();
    }
}

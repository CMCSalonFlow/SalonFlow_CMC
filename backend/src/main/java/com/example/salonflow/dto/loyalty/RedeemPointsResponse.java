package com.example.salonflow.dto.loyalty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemPointsResponse {
    private String voucherCode;
    private BigDecimal discountValue;
    private Integer pointsRedeemed;
    private Integer remainingPoints;
}

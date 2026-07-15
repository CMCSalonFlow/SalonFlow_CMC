package com.example.salonflow.dto.cancellation;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPolicyResponse {
    private Long id;
    private Long salonId;
    private Integer freeCancelHours;
    private BigDecimal feePercentage;
    private Boolean isActive;
}
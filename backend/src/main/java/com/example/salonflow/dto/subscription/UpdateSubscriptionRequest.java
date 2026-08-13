package com.example.salonflow.dto.subscription;

import com.example.salonflow.entity.enums.BillingCycle;
import com.example.salonflow.entity.enums.SubscriptionPlan;
import com.example.salonflow.entity.enums.SubscriptionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSubscriptionRequest {
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private BigDecimal price;
    private BillingCycle billingCycle;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Limits
    private Integer maxBranches;
    private Integer maxStaff;
    private Boolean analyticsAdvanced;
    private Boolean aiFeatures;
}

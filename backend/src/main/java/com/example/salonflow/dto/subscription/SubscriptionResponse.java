package com.example.salonflow.dto.subscription;

import com.example.salonflow.entity.SubscriptionFeatures;
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
public class SubscriptionResponse {
    private Long id;
    private Long salonId;
    private String salonName;
    private SubscriptionPlan plan;
    private SubscriptionFeatures features;
    private BillingCycle billingCycle;
    private BigDecimal price;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String stripeSubscriptionId;
    private String stripeCustomerId;
    private LocalDateTime createdAt;
}

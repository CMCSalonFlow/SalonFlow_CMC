package com.example.salonflow.dto.subscription;

import com.example.salonflow.entity.enums.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanConfigResponse {
    private Long id;
    private SubscriptionPlan plan;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private Integer maxBranches;
    private Integer maxStaffPerBranch;
    private Boolean hasAnalytics;
    private Boolean hasAi;
    private List<String> features;
    private String badgeText;
    private Boolean isPopular;
}

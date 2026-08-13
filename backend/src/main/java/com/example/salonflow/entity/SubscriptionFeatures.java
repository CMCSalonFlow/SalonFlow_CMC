package com.example.salonflow.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SubscriptionFeatures implements Serializable {
    
    private int maxBranches;
    private int maxStaff;
    private boolean analyticsAdvanced;
    private boolean aiFeatures;

    public static SubscriptionFeatures freeDefaults() {
        return SubscriptionFeatures.builder()
                .maxBranches(1)
                .maxStaff(3)
                .analyticsAdvanced(false)
                .aiFeatures(false)
                .build();
    }

    public static SubscriptionFeatures proDefaults() {
        return SubscriptionFeatures.builder()
                .maxBranches(3)
                .maxStaff(10)
                .analyticsAdvanced(true)
                .aiFeatures(false)
                .build();
    }

    public static SubscriptionFeatures enterpriseDefaults() {
        return SubscriptionFeatures.builder()
                .maxBranches(999) // Unlimited
                .maxStaff(999)    // Unlimited
                .analyticsAdvanced(true)
                .aiFeatures(true)
                .build();
    }
}

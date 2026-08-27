package com.example.salonflow.services.service;

import com.example.salonflow.dto.subscription.SubscriptionPlanConfigResponse;
import com.example.salonflow.dto.subscription.UpdateSubscriptionPlanConfigRequest;
import com.example.salonflow.entity.enums.SubscriptionPlan;

import java.util.List;

public interface SubscriptionPlanConfigService {
    List<SubscriptionPlanConfigResponse> getAllConfigs();
    SubscriptionPlanConfigResponse getConfigByPlan(SubscriptionPlan plan);
    SubscriptionPlanConfigResponse updateConfig(SubscriptionPlan plan, UpdateSubscriptionPlanConfigRequest request);
}

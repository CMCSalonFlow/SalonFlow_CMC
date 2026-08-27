package com.example.salonflow.services.impl;

import com.example.salonflow.dto.subscription.SubscriptionPlanConfigResponse;
import com.example.salonflow.dto.subscription.UpdateSubscriptionPlanConfigRequest;
import com.example.salonflow.entity.SubscriptionPlanConfig;
import com.example.salonflow.entity.enums.SubscriptionPlan;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.SubscriptionPlanConfigRepository;
import com.example.salonflow.services.service.SubscriptionPlanConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanConfigServiceImpl implements SubscriptionPlanConfigService {

    private final SubscriptionPlanConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanConfigResponse> getAllConfigs() {
        return configRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanConfigResponse getConfigByPlan(SubscriptionPlan plan) {
        SubscriptionPlanConfig config = configRepository.findByPlan(plan)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan config not found for plan: " + plan));
        return mapToResponse(config);
    }

    @Override
    @Transactional
    public SubscriptionPlanConfigResponse updateConfig(SubscriptionPlan plan, UpdateSubscriptionPlanConfigRequest request) {
        SubscriptionPlanConfig config = configRepository.findByPlan(plan)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan config not found for plan: " + plan));

        if (request.getName() != null) config.setName(request.getName());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getMonthlyPrice() != null) config.setMonthlyPrice(request.getMonthlyPrice());
        if (request.getYearlyPrice() != null) config.setYearlyPrice(request.getYearlyPrice());
        if (request.getMaxBranches() != null) config.setMaxBranches(request.getMaxBranches());
        if (request.getMaxStaffPerBranch() != null) config.setMaxStaffPerBranch(request.getMaxStaffPerBranch());
        if (request.getHasAnalytics() != null) config.setHasAnalytics(request.getHasAnalytics());
        if (request.getHasAi() != null) config.setHasAi(request.getHasAi());
        if (request.getBadgeText() != null) config.setBadgeText(request.getBadgeText());
        if (request.getIsPopular() != null) config.setIsPopular(request.getIsPopular());

        if (request.getFeatures() != null) {
            try {
                config.setFeaturesJson(objectMapper.writeValueAsString(request.getFeatures()));
            } catch (Exception e) {
                log.error("Error serializing features to JSON", e);
            }
        }

        config = configRepository.save(config);
        log.info("Updated subscription plan config for plan {}: {}", plan, config);
        return mapToResponse(config);
    }

    private SubscriptionPlanConfigResponse mapToResponse(SubscriptionPlanConfig config) {
        List<String> features = new ArrayList<>();
        if (config.getFeaturesJson() != null && !config.getFeaturesJson().isBlank()) {
            try {
                features = objectMapper.readValue(config.getFeaturesJson(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.error("Error parsing featuresJson", e);
            }
        }

        return SubscriptionPlanConfigResponse.builder()
                .id(config.getId())
                .plan(config.getPlan())
                .name(config.getName())
                .description(config.getDescription())
                .monthlyPrice(config.getMonthlyPrice())
                .yearlyPrice(config.getYearlyPrice())
                .maxBranches(config.getMaxBranches())
                .maxStaffPerBranch(config.getMaxStaffPerBranch())
                .hasAnalytics(config.getHasAnalytics())
                .hasAi(config.getHasAi())
                .features(features)
                .badgeText(config.getBadgeText())
                .isPopular(config.getIsPopular())
                .build();
    }
}

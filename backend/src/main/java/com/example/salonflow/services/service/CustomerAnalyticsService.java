package com.example.salonflow.services.service;

import com.example.salonflow.ai.dto.campaign.AiCampaignSuggestionRequest;
import com.example.salonflow.ai.dto.campaign.AiCampaignSuggestionResponse;
import com.example.salonflow.dto.analytics.*;
import com.example.salonflow.entity.TargetedCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerAnalyticsService {
    CustomerSegmentationOverviewResponse getCustomerSegmentationOverview(Long branchId);
    CustomerFunnelAnalyticsResponse getCustomerConversionFunnel(Long branchId);
    Page<CustomerSegmentDetailDto> getCustomersBySegment(Long branchId, String segmentType, String searchQuery, Pageable pageable);
    
    AiCampaignSuggestionResponse generateAiCampaignSuggestion(AiCampaignSuggestionRequest request);
    TargetedCampaign executeTargetedCampaign(TargetedCampaignCreateRequest request);
    List<TargetedCampaign> getCampaignHistory(Long branchId);
}

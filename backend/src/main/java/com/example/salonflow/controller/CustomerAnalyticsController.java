package com.example.salonflow.controller;

import com.example.salonflow.ai.dto.campaign.AiCampaignSuggestionRequest;
import com.example.salonflow.ai.dto.campaign.AiCampaignSuggestionResponse;
import com.example.salonflow.dto.analytics.*;
import com.example.salonflow.entity.TargetedCampaign;
import com.example.salonflow.services.service.CustomerAnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner/analytics/customers")
@RequiredArgsConstructor
public class CustomerAnalyticsController {

    private final CustomerAnalyticsService customerAnalyticsService;

    /**
     * Lấy tổng quan các chỉ số phân loại khách hàng (New, Returning, VIP, At-risk), CLV, AOV & Tần suất
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public CustomerSegmentationOverviewResponse getSegmentationOverview(
            @RequestParam(required = false) Long branchId
    ) {
        return customerAnalyticsService.getCustomerSegmentationOverview(branchId);
    }

    /**
     * Lấy dữ liệu phễu chuyển đổi khách hàng (Conversion Funnel)
     */
    @GetMapping("/funnel")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public CustomerFunnelAnalyticsResponse getConversionFunnel(
            @RequestParam(required = false) Long branchId
    ) {
        return customerAnalyticsService.getCustomerConversionFunnel(branchId);
    }

    /**
     * Lấy danh sách khách hàng phân trang theo phân khúc (New, Returning, VIP, At-risk)
     */
    @GetMapping("/segments")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public Page<CustomerSegmentDetailDto> getCustomersBySegment(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false, defaultValue = "ALL") String segmentType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return customerAnalyticsService.getCustomersBySegment(branchId, segmentType, search, PageRequest.of(page, size));
    }

    /**
     * AI hỗ trợ gợi ý thông điệp chiến dịch tiếp thị & đề xuất voucher phù hợp theo từng Segment
     */
    @PostMapping("/campaigns/ai-generate")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public AiCampaignSuggestionResponse generateAiCampaign(
            @RequestBody AiCampaignSuggestionRequest request
    ) {
        return customerAnalyticsService.generateAiCampaignSuggestion(request);
    }

    /**
     * Tạo và phát hành chiến dịch tiếp thị nhắm mục tiêu (tự động sinh voucher & gửi notification hàng loạt)
     */
    @PostMapping("/campaigns/execute")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public TargetedCampaign executeCampaign(
            @Valid @RequestBody TargetedCampaignCreateRequest request
    ) {
        return customerAnalyticsService.executeTargetedCampaign(request);
    }

    /**
     * Lấy lịch sử các chiến dịch tiếp thị nhắm mục tiêu đã chạy
     */
    @GetMapping("/campaigns/history")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public List<TargetedCampaign> getCampaignHistory(
            @RequestParam(required = false) Long branchId
    ) {
        return customerAnalyticsService.getCampaignHistory(branchId);
    }
}

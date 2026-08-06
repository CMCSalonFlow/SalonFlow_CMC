package com.example.salonflow.controller;

import com.example.salonflow.dto.recommendation.RecommendationResponse;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * API GET /api/v1/recommendations?user_id=&branch_id=&limit=5&ab_group=
     * AI gợi ý dịch vụ dựa trên lịch sử đặt lịch (Collaborative Filtering Cosine Similarity) + Popularity Fallback + A/B Testing + 1h Cache.
     */
    @GetMapping
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @RequestParam(name = "user_id", required = false) Long userIdParam,
            @RequestParam(name = "userId", required = false) Long userIdAlias,
            @RequestParam(name = "branch_id", required = false) Long branchIdParam,
            @RequestParam(name = "branchId", required = false) Long branchIdAlias,
            @RequestParam(name = "limit", required = false, defaultValue = "5") Integer limit,
            @RequestParam(name = "ab_group", required = false) String abGroupParam,
            @RequestParam(name = "abGroup", required = false) String abGroupAlias
    ) {
        Long targetUserId = userIdParam != null ? userIdParam : userIdAlias;

        // Nếu không truyền user_id param, cố gắng lấy từ Token hiện tại nếu đã đăng nhập
        if (targetUserId == null) {
            try {
                targetUserId = SecurityUtils.getCurrentUserId();
            } catch (Exception ignored) {
                targetUserId = 0L;
            }
        }

        Long targetBranchId = branchIdParam != null ? branchIdParam : branchIdAlias;
        String forceAbGroup = abGroupParam != null ? abGroupParam : abGroupAlias;

        RecommendationResponse response = recommendationService.getRecommendations(
                targetUserId,
                targetBranchId,
                limit,
                forceAbGroup
        );

        return ResponseEntity.ok(response);
    }
}

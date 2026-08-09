package com.example.salonflow.services.service;

import com.example.salonflow.dto.recommendation.RecommendationResponse;

public interface RecommendationService {

    /**
     * Gợi ý dịch vụ cho người dùng (Collaborative Filtering AI + Popularity Fallback + A/B Testing + 1 Hour Cache)
     *
     * @param userId ID khách hàng cần gợi ý
     * @param branchId ID chi nhánh (tùy chọn)
     * @param limit Số lượng dịch vụ tối đa (mặc định 5)
     * @param forceAbGroup Nhóm A/B ép buộc (TREATMENT / CONTROL, nếu null tự động phân nhóm)
     * @return RecommendationResponse kết quả gợi ý
     */
    RecommendationResponse getRecommendations(Long userId, Long branchId, Integer limit, String forceAbGroup);
}

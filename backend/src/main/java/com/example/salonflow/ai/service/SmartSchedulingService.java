package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.scheduling.*;
import com.example.salonflow.entity.SmartSchedulingLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SmartSchedulingService {

    /**
     * Tính toán và đề xuất Top 3 slot tối ưu nhất cho khách hàng dựa trên Rule-based + ML scoring.
     */
    SmartSchedulingResponse recommendSlots(SmartSchedulingRequest request);

    /**
     * Lấy cấu hình trọng số gợi ý hiện tại của chi nhánh hoặc toàn hệ thống.
     */
    SmartSchedulingConfigDto getConfig(Long branchId);

    /**
     * Admin/Owner cập nhật tinh chỉnh trọng số gợi ý.
     */
    SmartSchedulingConfigDto updateConfig(Long branchId, UpdateSmartSchedulingConfigDto dto);

    /**
     * Truy vấn danh sách log đề xuất slot để đánh giá hiệu quả.
     */
    Page<SmartSchedulingLog> getRecommendationLogs(Long branchId, Pageable pageable);
}

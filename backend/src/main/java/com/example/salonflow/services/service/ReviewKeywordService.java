package com.example.salonflow.services.service;

import com.example.salonflow.dto.reviewanalytics.WordCloudResponse;

import java.time.YearMonth;

public interface ReviewKeywordService {

    /**
     * Batch job: tính lại tần suất từ khoá cho TẤT CẢ branch, cho 1 tháng cụ thể.
     * Được gọi tự động theo lịch (@Scheduled) hoặc có thể gọi thủ công để backfill.
     */
    void recomputeKeywordsForMonth(YearMonth yearMonth);

    /**
     * Lấy word cloud đã tính sẵn (KHÔNG tính real-time).
     * Chỉ 1 trong 2 tham số salonId / branchId được truyền (branchId ưu tiên nếu có cả 2).
     */
    WordCloudResponse getWordCloud(Long salonId, Long branchId, YearMonth yearMonth, int limit);
}

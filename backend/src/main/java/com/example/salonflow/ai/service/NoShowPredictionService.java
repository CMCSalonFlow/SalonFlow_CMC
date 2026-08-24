package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.noshow.*;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.NoShowModelConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoShowPredictionService {

    /**
     * Thực hiện tính toán AI dự đoán No-Show cho 1 booking và lưu log.
     */
    NoShowPredictionDto predictAndSaveLog(Booking booking);

    /**
     * Lấy thông tin dự đoán No-Show của booking theo ID.
     */
    NoShowPredictionDto getPredictionByBookingId(Long bookingId);

    /**
     * Lấy danh sách booking có nguy cơ No-Show cao (riskLevel = HIGH) của 1 chi nhánh.
     */
    Page<NoShowPredictionDto> getHighRiskBookings(Long branchId, Pageable pageable);

    /**
     * Lấy lịch sử tất cả log dự đoán No-Show của chi nhánh.
     */
    Page<NoShowPredictionDto> getPredictionLogs(Long branchId, Pageable pageable);

    /**
     * Lấy cấu hình tham số mô hình Logistic Regression hiện tại.
     */
    NoShowModelConfig getModelConfig();

    /**
     * Cập nhật cấu hình trọng số Logistic Regression và ngưỡng cảnh báo.
     */
    NoShowModelConfig updateModelConfig(UpdateNoShowModelConfigDto dto);

    /**
     * Tự động/thủ công gửi Email nhắc nhở cho khách.
     */
    boolean sendManualReminder(Long bookingId);
}

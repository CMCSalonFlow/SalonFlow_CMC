package com.example.salonflow.services.service;

import com.example.salonflow.dto.recurring.*;

public interface RecurringBookingService {

    /**
     * Tính toán trước (KHÔNG lưu DB) danh sách các ngày sẽ được đặt
     * theo pattern lặp, đánh dấu ngày nào bị conflict.
     * FE dùng kết quả này để render calendar preview.
     */
    RecurringBookingPreviewResponse preview(
            Long customerId,
            RecurringBookingRequest request
    );

    /**
     * Tạo thật các booking sau khi user đã xem preview và quyết định
     * xử lý từng ngày bị conflict (SKIP hoặc đổi giờ).
     */
    RecurringBookingResponse confirm(
            Long customerId,
            RecurringBookingConfirmRequest request
    );

    /**
     * Hủy toàn bộ chuỗi recurring booking (các booking chưa diễn ra).
     */
    void cancelRecurring(Long customerId, Long recurringBookingId);
}

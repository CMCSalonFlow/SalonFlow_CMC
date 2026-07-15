package com.example.salonflow.dto.booking;

import lombok.*;

import java.math.BigDecimal;

/**
 * Phản hồi chi tiết từng dịch vụ hoặc combo nằm trong đơn đặt chỗ (BookingItemResponse).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItemResponse {

    private Long id;

    // Thông tin dịch vụ lẻ (nếu có)
    private Long serviceId;
    private String serviceName;

    // Thông tin combo/gói dịch vụ (nếu có)
    private Long bundleId;
    private String bundleName;

    // Giá tại thời điểm đặt lịch
    private BigDecimal price;

    // Thời lượng thực hiện (phút) tại thời điểm đặt lịch
    private Integer durationMinutes;
}

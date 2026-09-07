package com.example.salonflow.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Request lock slot khi user chọn khung giờ.
 * Backend dùng Redis SETNX để lock slot trong 5 phút.
 */
@Data
public class LockSlotRequest {

    @NotNull(message = "Branch không được để trống")
    private Long branchId;

    // Nhân viên (nếu khách chọn cụ thể, có thể null nếu chọn bất kỳ)
    private Long staffId;

    // Dịch vụ đơn lẻ
    private Long serviceId;

    // Danh sách dịch vụ nếu chọn nhiều
    private List<Long> serviceIds;

    // Combo dịch vụ nếu chọn combo
    private Long bundleId;

    @NotNull(message = "Ngày đặt không được để trống")
    private LocalDate bookingDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    // Client ID (UUID) nếu là khách vãng lai (Guest)
    private String clientId;

    // Thời lượng dịch vụ (phút) để tính toán giữ các sub-slot 15 phút tiếp theo
    private Integer durationMinutes;

    // Key slot trước đó để tự động hủy khi đổi giờ
    private String previousSlotKey;
}

package com.example.salonflow.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Yêu cầu tạo mới lịch hẹn đặt chỗ (CreateBookingRequest).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingRequest {

    // ID của khách hàng (nếu trống, hệ thống sẽ tự động lấy thông tin người dùng đang đăng nhập)
    private Long customerId;

    // Ngày hẹn dịch vụ
    @NotNull(message = "Ngày hẹn không được để trống")
    private LocalDate bookingDate;

    // Giờ bắt đầu hẹn
    @NotNull(message = "Giờ hẹn không được để trống")
    private LocalTime startTime;

    // ID nhân viên mong muốn (nullable - nếu NULL nghĩa là chọn "Bất kỳ nhân viên")
    private Long preferredStaffId;

    // Danh sách ID các dịch vụ lẻ được đặt (nullable nếu đặt theo combo)
    private List<Long> serviceIds;

    // ID của combo/gói dịch vụ được đặt (nullable nếu đặt dịch vụ lẻ)
    private Long bundleId;

    // Ghi chú thêm từ phía khách hàng
    private String notes;
}

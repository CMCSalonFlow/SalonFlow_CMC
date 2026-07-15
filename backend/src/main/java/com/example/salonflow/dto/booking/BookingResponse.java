package com.example.salonflow.dto.booking;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Phản hồi chi tiết thông tin đặt lịch hẹn (BookingResponse) trả về cho phía giao diện (FE).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;

    // Khách hàng đặt lịch
    private Long customerId;
    private String customerName;
    private String customerPhone;

    // Chi nhánh
    private Long branchId;
    private String branchName;

    // Thời gian đặt hẹn
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // Nhân viên yêu thích và nhân viên thực tế thực hiện
    private Long preferredStaffId;
    private String preferredStaffName;
    private Long assignedStaffId;
    private String assignedStaffName;

    // Trạng thái đơn đặt và thông tin tổng tiền/thời gian
    private String status;
    private BigDecimal totalPrice;
    private BigDecimal depositAmount;
    private Integer totalDurationMinutes;
    private String notes;

    private String invoiceUrl;
    private LocalDateTime invoiceGeneratedAt;

    // Chi tiết danh sách dịch vụ trong đơn đặt lịch
    private List<BookingItemResponse> items;
}

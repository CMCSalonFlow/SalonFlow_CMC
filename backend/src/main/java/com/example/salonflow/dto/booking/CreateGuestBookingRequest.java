package com.example.salonflow.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Yêu cầu tạo booking cho guest, không cần customerId và không cần đăng nhập.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGuestBookingRequest {

    @NotBlank(message = "Tên khách không được để trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại khách không được để trống")
    private String customerPhone;

    private String customerEmail;

    @NotNull(message = "Ngày hẹn không được để trống")
    private LocalDate bookingDate;

    @NotNull(message = "Giờ hẹn không được để trống")
    private LocalTime startTime;

    private Long preferredStaffId;

    private List<Long> serviceIds;

    private Long bundleId;

    private String notes;
}

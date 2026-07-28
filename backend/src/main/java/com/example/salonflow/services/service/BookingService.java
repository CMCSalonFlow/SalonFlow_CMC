package com.example.salonflow.services.service;

import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.dto.booking.CreateGuestBookingRequest;
import com.example.salonflow.dto.booking.CreateBookingRequest;
import com.example.salonflow.dto.booking.BookingResponse;
import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.dto.booking.CreateWalkInBookingRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Giao diện định nghĩa các nghiệp vụ liên quan đến Đặt lịch (Booking).
 */
public interface BookingService {

    // Tạo mới một lịch hẹn đặt chỗ tại chi nhánh
    BookingResponse create(Long branchId, CreateBookingRequest request);

    // Tạo booking cho guest, không cần customerId và không cần đăng nhập
    BookingResponse createGuestBooking(Long branchId, CreateGuestBookingRequest request);

    // Lấy danh sách lịch hẹn đặt chỗ của chi nhánh
    List<BookingResponse> getByBranch(Long branchId);

    // Lấy thông tin chi tiết của một lịch hẹn đặt chỗ
    BookingResponse getById(Long branchId, Long bookingId);

    // Kiểm tra tính rảnh (Availability) của nhân viên hoặc chi nhánh theo thời gian thực
    AvailabilityResponse getAvailability(Long branchId, LocalDate date, List<Long> serviceIds, Long bundleId, Long staffId);

    // Tạo booking tại quầy (Walk-in)
    BookingResponse createWalkInBooking( Long branchId, CreateWalkInBookingRequest request);

    CancellationResult cancelBooking(Long bookingId, String reason);

    void cancelUnpaidBookings();

    BookingResponse completeBooking(Long bookingId);

    BookingResponse confirmBooking(Long bookingId);
}

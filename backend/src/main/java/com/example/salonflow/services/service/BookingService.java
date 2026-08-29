package com.example.salonflow.services.service;

import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.dto.booking.CreateGuestBookingRequest;
import com.example.salonflow.dto.booking.CreateBookingRequest;
import com.example.salonflow.dto.booking.BookingResponse;
import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.dto.booking.CheckInBookingResponse;
import com.example.salonflow.dto.booking.CreateWalkInBookingRequest;

import com.example.salonflow.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    // Lấy danh sách lịch hẹn của một khách hàng
    List<BookingResponse> getByCustomerId(Long customerId);

    // Phân trang & tìm kiếm danh sách lịch hẹn đặt chỗ của chi nhánh
    Page<BookingResponse> searchBookings(Long branchId, BookingStatus status, LocalDate fromDate, LocalDate toDate, String search, Pageable pageable);

    // Lấy thông tin chi tiết của một lịch hẹn đặt chỗ
    BookingResponse getById(Long branchId, Long bookingId);

    // Kiểm tra tính rảnh (Availability) của nhân viên hoặc chi nhánh theo thời gian thực
    AvailabilityResponse getAvailability(Long branchId, LocalDate date, List<Long> serviceIds, Long bundleId, Long staffId);

    // Tạo booking tại quầy (Walk-in)
    BookingResponse createWalkInBooking( Long branchId, CreateWalkInBookingRequest request);

    CancellationResult cancelBooking(Long bookingId, String reason);

    void cancelUnpaidBookings();

    BookingResponse completeBooking(Long bookingId);

    BookingResponse checkInBooking(Long bookingId);

    CheckInBookingResponse checkInBookingByQr(Long bookingId, String signature);

    BookingResponse confirmBooking(Long bookingId);
}

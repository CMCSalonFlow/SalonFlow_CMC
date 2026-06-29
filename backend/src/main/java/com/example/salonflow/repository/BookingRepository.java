package com.example.salonflow.repository;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

/**
 * Kho lưu trữ truy vấn cơ sở dữ liệu cho thực thể Lịch hẹn (Booking).
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Tìm các lịch hẹn của một chi nhánh theo ngày và danh sách các trạng thái
    List<Booking> findByBranchIdAndBookingDateAndStatusIn(Long branchId, LocalDate bookingDate, Collection<BookingStatus> statuses);

    // Tìm các lịch hẹn của một nhân viên cụ thể theo ngày và danh sách các trạng thái
    List<Booking> findByAssignedStaffIdAndBookingDateAndStatusIn(Long staffId, LocalDate bookingDate, Collection<BookingStatus> statuses);

    // Truy vấn kiểm tra trùng lịch của một nhân viên trong một khung giờ cụ thể
    @Query("SELECT b FROM Booking b WHERE b.assignedStaff.id = :staffId " +
           "AND b.bookingDate = :date " +
           "AND b.status IN :statuses " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findOverlappingBookings(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<BookingStatus> statuses
    );
}

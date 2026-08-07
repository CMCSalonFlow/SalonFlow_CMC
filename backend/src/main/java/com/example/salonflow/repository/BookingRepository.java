package com.example.salonflow.repository;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Kho lưu trữ truy vấn cơ sở dữ liệu cho thực thể Lịch hẹn (Booking).
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Tìm các lịch hẹn của một chi nhánh theo ngày và danh sách các trạng thái
    List<Booking> findByBranchIdAndBookingDateAndStatusIn(Long branchId, LocalDate bookingDate, Collection<BookingStatus> statuses);

    // Tìm các lịch hẹn của một nhân viên cụ thể theo ngày và danh sách các trạng thái
    List<Booking> findByAssignedStaffIdAndBookingDateAndStatusIn(Long staffId, LocalDate bookingDate, Collection<BookingStatus> statuses);

    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.status = :status
          AND (
                b.bookingDate < :date
                OR (
                    b.bookingDate = :date
                    AND b.startTime < :time
                )
          )
    """)
    List<Booking> findExpiredBookings(
            @Param("status") BookingStatus status,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = :status
          AND (
                b.bookingDate < :today
                OR (b.bookingDate = :today AND b.startTime <= :nowTime)
          )
    """)
    List<Booking> findExpiredPendingBookings(
            @Param("status") BookingStatus status,
            @Param("today") LocalDate today,
            @Param("nowTime") LocalTime nowTime
    );

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

    // Tìm booking bị conflict với ngày nghỉ 
    @Query("SELECT b FROM Booking b WHERE b.assignedStaff.id = :staffId " +
           "AND b.status <> 'CANCELLED' " +
           "AND b.bookingDate >= :startDate " +
           "AND b.bookingDate <= :endDate")
    List<Booking> findConflictingBookingsWithOffDay(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Booking> findByCustomerId(Long customerId);

    List<Booking> findByAssignedStaffIdAndBookingDate(Long staffId, LocalDate date);

    List<Booking> findByBranchIdAndBookingDate(Long branchId, LocalDate date);

    Optional<Booking> findBySlotKey(String slotKey);

    List<Booking> findByStatus(BookingStatus status);

    /**
     * THÊM MỚI cho recurring booking: kiểm tra 1 staff có booking
     * trùng giờ vào 1 ngày cụ thể không (dùng khi preview/confirm
     * từng occurrence trong chuỗi lặp).
     *
     * Điều kiện overlap: start1 < end2 AND end1 > start2
     * Chỉ tính các booking chưa bị hủy (status != CANCELLED).
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.assignedStaff.id = :staffId
            AND b.bookingDate = :date
            AND b.status != 'CANCELLED'
            AND b.startTime < :endTime
            AND b.endTime > :startTime
            """)
    boolean existsConflict(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    List<Booking> findByRecurringBookingId(Long recurringBookingId);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' " +
           "AND b.createdAt < :cutoffTime " +
           "AND EXISTS (SELECT p FROM Payment p WHERE p.booking = b AND p.status IN ('PENDING', 'FAILED'))")
    List<Booking> findUnpaidOnlineBookings(@Param("cutoffTime") java.time.Instant cutoffTime);

    // Queries cho Analytics
    List<Booking> findByBranchSalonIdAndBookingDateBetween(Long salonId, LocalDate startDate, LocalDate endDate);

    List<Booking> findByBranchIdAndBookingDateBetween(Long branchId, LocalDate startDate, LocalDate endDate);

    List<Booking> findByBranchSalonIdAndBookingDate(Long salonId, LocalDate date);
}


package com.example.salonflow.repository;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerId(Long customerId);

    List<Booking> findByStaffIdAndBookingDate(Long staffId, LocalDate date);

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
            WHERE b.staff.id = :staffId
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
}

package com.example.salonflow.repository;

import com.example.salonflow.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Kho lưu trữ truy vấn cơ sở dữ liệu cho thực thể Chi tiết dịch vụ đặt (BookingItem).
 */
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    @Query("""
        SELECT bi FROM BookingItem bi
        WHERE bi.booking.branch.salon.id = :salonId
          AND bi.booking.status = 'COMPLETED'
          AND bi.booking.bookingDate BETWEEN :startDate AND :endDate
    """)
    List<BookingItem> findCompletedItemsBySalonIdAndDateRange(
            @Param("salonId") Long salonId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT bi FROM BookingItem bi
        WHERE bi.booking.branch.id = :branchId
          AND bi.booking.status = 'COMPLETED'
          AND bi.booking.bookingDate BETWEEN :startDate AND :endDate
    """)
    List<BookingItem> findCompletedItemsByBranchIdAndDateRange(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

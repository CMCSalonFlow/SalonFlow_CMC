package com.example.salonflow.repository;

import com.example.salonflow.dto.recommendation.UserServiceUsageProjection;
import com.example.salonflow.entity.BookingItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Kho lưu trữ truy vấn cơ sở dữ liệu cho thực thể Chi tiết dịch vụ đặt
 * (BookingItem).
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
            @Param("endDate") LocalDate endDate);

    @Query("""
                SELECT bi FROM BookingItem bi
                WHERE bi.booking.branch.id = :branchId
                  AND bi.booking.status = 'COMPLETED'
                  AND bi.booking.bookingDate BETWEEN :startDate AND :endDate
            """)
    List<BookingItem> findCompletedItemsByBranchIdAndDateRange(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Lấy danh sách tần suất sử dụng dịch vụ của tất cả khách hàng (phục vụ
     * Collaborative Filtering AI).
     */
    @Query("SELECT b.customer.id AS userId, bi.service.id AS serviceId, COUNT(bi) AS usageCount " +
            "FROM BookingItem bi JOIN bi.booking b " +
            "WHERE b.customer.id IS NOT NULL AND bi.service.id IS NOT NULL AND b.status IN ('COMPLETED', 'CONFIRMED') "
            +
            "GROUP BY b.customer.id, bi.service.id")
    List<UserServiceUsageProjection> findUserServiceUsageVectors();

    /**
     * Lấy các dịch vụ phổ biến nhất hệ thống (Fallback khi không có history hoặc
     * nhóm Control).
     */
    @Query("SELECT bi.service.id " +
            "FROM BookingItem bi JOIN bi.booking b " +
            "WHERE bi.service.id IS NOT NULL AND b.status IN ('COMPLETED', 'CONFIRMED') " +
            "GROUP BY bi.service.id " +
            "ORDER BY COUNT(bi) DESC")
    List<Long> findTopPopularServiceIds(Pageable pageable);

    /**
     * Lấy các dịch vụ phổ biến nhất theo chi nhánh (Fallback theo khu vực).
     */
    @Query("SELECT bi.service.id " +
            "FROM BookingItem bi JOIN bi.booking b " +
            "WHERE bi.service.id IS NOT NULL AND b.status IN ('COMPLETED', 'CONFIRMED') AND b.branch.id = :branchId " +
            "GROUP BY bi.service.id " +
            "ORDER BY COUNT(bi) DESC")
    List<Long> findTopPopularServiceIdsByBranch(@Param("branchId") Long branchId, Pageable pageable);
}

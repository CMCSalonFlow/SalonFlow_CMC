package com.example.salonflow.repository;

import com.example.salonflow.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findByUserIdAndShiftDate(Long userId, LocalDate date);

    List<Shift> findByBranchIdAndShiftDate(Long branchId, LocalDate date);

    List<Shift> findByUserIdAndShiftDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Shift> findByBranchIdAndShiftDateBetween(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Kiểm tra overlap shift của 1 user trong 1 ngày.
     * Điều kiện overlap: start1 < end2 AND end1 > start2
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM Shift s
            WHERE s.user.id = :userId
            AND s.branch.id = :branchId
            AND s.shiftDate = :date
            AND s.status != 'CANCELLED'
            AND s.startTime < :endTime
            AND s.endTime > :startTime
            AND (:excludeId IS NULL OR s.id != :excludeId)
            """)
    boolean existsOverlappingShift(
            @Param("userId") Long userId,
            @Param("branchId") Long branchId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    /**
     * Lấy tất cả shift đang active của 1 branch trong 1 ngày
     * để tính availability slots cho booking.
     */
    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.user
            WHERE s.branch.id = :branchId
            AND s.shiftDate = :date
            AND s.status = 'SCHEDULED'
            ORDER BY s.user.id, s.startTime
            """)
    List<Shift> findScheduledShiftsByBranchAndDate(
            @Param("branchId") Long branchId,
            @Param("date") LocalDate date
    );
}

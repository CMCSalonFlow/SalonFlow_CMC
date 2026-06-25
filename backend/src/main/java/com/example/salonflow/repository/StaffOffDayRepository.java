package com.example.salonflow.repository;

import com.example.salonflow.entity.StaffOffDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StaffOffDayRepository extends JpaRepository<StaffOffDay, Long> {

    List<StaffOffDay> findByStaffId(Long staffId);

    List<StaffOffDay> findByStaffIdOrderByDateFromDesc(Long staffId);

    // Kiểm tra xem nhân viên có off trong khoảng thời gian không
    boolean existsByStaffIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(
            Long staffId, LocalDate dateFrom, LocalDate dateTo);

    // Tìm tất cả off day trong khoảng thời gian (dùng cho calendar)
    @Query("SELECT s FROM StaffOffDay s WHERE s.staff.id = :staffId " +
           "AND s.dateFrom <= :endDate AND s.dateTo >= :startDate")
    List<StaffOffDay> findOffDaysInRange(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Tìm các off day chồng chéo với khoảng thời gian mới
    @Query("SELECT s FROM StaffOffDay s WHERE s.staff.id = :staffId " +
           "AND s.id <> :excludeId " +
           "AND s.dateFrom <= :dateTo AND s.dateTo >= :dateFrom")
    List<StaffOffDay> findOverlappingOffDays(
            @Param("staffId") Long staffId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("excludeId") Long excludeId);
}
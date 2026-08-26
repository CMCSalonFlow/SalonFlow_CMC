package com.example.salonflow.repository;

import com.example.salonflow.entity.LeaveStatus;
import com.example.salonflow.entity.Staff;
import com.example.salonflow.entity.StaffOffDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffOffDayRepository extends JpaRepository<StaffOffDay, Long> {

    List<StaffOffDay> findByStaffId(Long staffId);

    List<StaffOffDay> findByStaffIdOrderByCreatedAtDesc(Long staffId);

    boolean existsByStaffIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(
            Long staffId, LocalDate dateFrom, LocalDate dateTo);

    @Query("SELECT s FROM StaffOffDay s WHERE s.staff.id = :staffId " +
           "AND (s.status = com.example.salonflow.entity.LeaveStatus.APPROVED OR s.status IS NULL) " +
           "AND s.dateFrom <= :dateTo AND s.dateTo >= :dateFrom")
    List<StaffOffDay> findApprovedOffDaysInRange(
            @Param("staffId") Long staffId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM StaffOffDay s " +
           "WHERE s.staff.id = :staffId " +
           "AND (s.status = com.example.salonflow.entity.LeaveStatus.APPROVED OR s.status IS NULL) " +
           "AND s.dateFrom <= :date AND s.dateTo >= :date")
    boolean isStaffApprovedOffOnDate(@Param("staffId") Long staffId, @Param("date") LocalDate date);

    @Query("SELECT s FROM StaffOffDay s WHERE s.staff.id = :staffId " +
           "AND s.status <> com.example.salonflow.entity.LeaveStatus.CANCELLED " +
           "AND s.status <> com.example.salonflow.entity.LeaveStatus.REJECTED " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId) " +
           "AND s.dateFrom <= :dateTo AND s.dateTo >= :dateFrom")
    List<StaffOffDay> findOverlappingActiveRequests(
            @Param("staffId") Long staffId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("excludeId") Long excludeId);

    // Query cho Salon Owner (xem tất cả chi nhánh thuộc Salon)
    @Query("SELECT s FROM StaffOffDay s WHERE s.staff.branch.salon.id = :salonId " +
           "AND (:branchId IS NULL OR s.staff.branch.id = :branchId) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.createdAt DESC")
    List<StaffOffDay> findOffDaysForOwner(
            @Param("salonId") Long salonId,
            @Param("branchId") Long branchId,
            @Param("status") LeaveStatus status);

    // Query cho Branch Manager (chỉ xem chi nhánh được gán)
    @Query("SELECT s FROM StaffOffDay s WHERE s.staff.branch.id = :branchId " +
           "AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.createdAt DESC")
    List<StaffOffDay> findOffDaysForManager(
            @Param("branchId") Long branchId,
            @Param("status") LeaveStatus status);

    List<StaffOffDay> findByStatusAndDateToGreaterThanEqualAndDateFromLessThanEqual(
            LeaveStatus status, LocalDate startDate, LocalDate endDate);

    List<StaffOffDay> findByStaff_Branch_IdAndStatusAndDateToGreaterThanEqualAndDateFromLessThanEqual(
            Long branchId, LeaveStatus status, LocalDate startDate, LocalDate endDate);
}
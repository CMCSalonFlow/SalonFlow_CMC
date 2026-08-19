package com.example.salonflow.repository;

import com.example.salonflow.entity.SystemOffDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SystemOffDayRepository extends JpaRepository<SystemOffDay, Long> {

    List<SystemOffDay> findBySalonIdOrderByDateFromDesc(Long salonId);

    @Query("SELECT s FROM SystemOffDay s WHERE (s.salon.id = :salonId OR :salonId IS NULL) AND (s.isAllBranches = true OR s.branch.id = :branchId) AND s.dateTo >= :startDate AND s.dateFrom <= :endDate")
    List<SystemOffDay> findOffDaysForBranchAndRange(
            @Param("salonId") Long salonId,
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT s FROM SystemOffDay s WHERE s.branch.id = :branchId AND s.dateTo >= :startDate AND s.dateFrom <= :endDate")
    List<SystemOffDay> findOffDaysByBranchAndRange(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

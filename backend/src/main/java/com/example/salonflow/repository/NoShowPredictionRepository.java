package com.example.salonflow.repository;

import com.example.salonflow.entity.NoShowPredictionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoShowPredictionRepository extends JpaRepository<NoShowPredictionLog, Long> {

    Optional<NoShowPredictionLog> findByBookingId(Long bookingId);

    List<NoShowPredictionLog> findByBookingIdIn(List<Long> bookingIds);

    Page<NoShowPredictionLog> findByBranchIdAndRiskLevelOrderByCreatedAtDesc(Long branchId, String riskLevel, Pageable pageable);

    Page<NoShowPredictionLog> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);

    @Query("SELECT p FROM NoShowPredictionLog p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate")
    List<NoShowPredictionLog> findLogsBetweenDates(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}

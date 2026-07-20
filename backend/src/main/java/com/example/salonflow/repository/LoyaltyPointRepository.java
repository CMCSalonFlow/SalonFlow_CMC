package com.example.salonflow.repository;

import com.example.salonflow.entity.LoyaltyPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoyaltyPointRepository extends JpaRepository<LoyaltyPoint, Long> {

    List<LoyaltyPoint> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(lp.points), 0) FROM LoyaltyPoint lp WHERE lp.userId = :userId AND (lp.expiresAt IS NULL OR lp.expiresAt > :now)")
    Integer findTotalPointsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(lp.points), 0) FROM LoyaltyPoint lp WHERE lp.userId = :userId AND lp.transactionType = 'EARN' AND lp.expiresAt IS NOT NULL AND lp.expiresAt BETWEEN :now AND :threshold")
    Integer findExpiringPointsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);

    @Query("SELECT lp FROM LoyaltyPoint lp WHERE lp.transactionType = 'EARN' AND lp.expiresAt IS NOT NULL AND lp.expiresAt <= :now AND lp.points > 0")
    List<LoyaltyPoint> findExpiredPoints(@Param("now") LocalDateTime now);
}

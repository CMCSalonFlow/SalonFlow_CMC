package com.example.salonflow.repository;

import com.example.salonflow.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long>, JpaSpecificationExecutor<Subscription> {

    @Query("SELECT s FROM Subscription s WHERE s.salon.id = :salonId AND s.status = 'ACTIVE' AND (s.endDate IS NULL OR s.endDate > :now) ORDER BY s.createdAt DESC")
    List<Subscription> findActiveSubscriptions(@Param("salonId") Long salonId, @Param("now") LocalDateTime now);

    List<Subscription> findBySalonIdOrderByCreatedAtDesc(Long salonId);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate IS NOT NULL AND s.endDate <= :now")
    List<Subscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}

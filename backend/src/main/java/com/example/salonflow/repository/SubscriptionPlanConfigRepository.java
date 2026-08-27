package com.example.salonflow.repository;

import com.example.salonflow.entity.SubscriptionPlanConfig;
import com.example.salonflow.entity.enums.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanConfigRepository extends JpaRepository<SubscriptionPlanConfig, Long> {
    Optional<SubscriptionPlanConfig> findByPlan(SubscriptionPlan plan);
}

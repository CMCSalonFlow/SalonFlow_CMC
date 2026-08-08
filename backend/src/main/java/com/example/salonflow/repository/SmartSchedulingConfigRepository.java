package com.example.salonflow.repository;

import com.example.salonflow.entity.SmartSchedulingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmartSchedulingConfigRepository extends JpaRepository<SmartSchedulingConfig, Long> {

    Optional<SmartSchedulingConfig> findByBranchId(Long branchId);

    Optional<SmartSchedulingConfig> findFirstByBranchIdIsNull();
}

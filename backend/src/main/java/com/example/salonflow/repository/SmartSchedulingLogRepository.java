package com.example.salonflow.repository;

import com.example.salonflow.entity.SmartSchedulingLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmartSchedulingLogRepository extends JpaRepository<SmartSchedulingLog, Long> {

    Page<SmartSchedulingLog> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);

    List<SmartSchedulingLog> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}

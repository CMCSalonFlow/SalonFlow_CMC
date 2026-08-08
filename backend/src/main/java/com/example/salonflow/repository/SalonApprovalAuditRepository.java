package com.example.salonflow.repository;

import com.example.salonflow.entity.SalonApprovalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalonApprovalAuditRepository extends JpaRepository<SalonApprovalAudit, Long> {
    List<SalonApprovalAudit> findBySalonIdOrderByCreatedAtDesc(Long salonId);
}

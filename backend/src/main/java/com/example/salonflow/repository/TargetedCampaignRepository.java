package com.example.salonflow.repository;

import com.example.salonflow.entity.TargetedCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetedCampaignRepository extends JpaRepository<TargetedCampaign, Long> {
    List<TargetedCampaign> findBySalonIdOrderByCreatedAtDesc(Long salonId);
    List<TargetedCampaign> findByBranchIdOrderByCreatedAtDesc(Long branchId);
}

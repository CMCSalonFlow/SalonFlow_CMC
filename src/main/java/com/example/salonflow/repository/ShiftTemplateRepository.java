package com.example.salonflow.repository;

import com.example.salonflow.entity.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {

    List<ShiftTemplate> findByUserIdAndBranchId(Long userId, Long branchId);

    List<ShiftTemplate> findByBranchId(Long branchId);

    Optional<ShiftTemplate> findByIdAndUserId(Long id, Long userId);

    List<ShiftTemplate> findByUserIdAndIsActiveTrue(Long userId);
}

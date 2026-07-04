package com.example.salonflow.repository;

import com.example.salonflow.entity.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<SalonService, Long> {

    List<SalonService> findByBranchId(Long branchId);

    List<SalonService> findByBranchIdAndIsActiveTrue(Long branchId);

    Optional<SalonService> findByIdAndBranchId(Long id, Long branchId);

    List<SalonService> findByCategoryId(Long categoryId);
}
package com.example.salonflow.repository;

import com.example.salonflow.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByBranchId(Long branchId);

    List<Service> findByBranchIdAndIsActiveTrue(Long branchId);

    Optional<Service> findByIdAndBranchId(Long id, Long branchId);

    List<Service> findByCategoryId(Long categoryId);
}
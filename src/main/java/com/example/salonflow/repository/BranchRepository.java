package com.example.salonflow.repository;

import com.example.salonflow.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    List<Branch> findBySalonId(Long salonId);

    Optional<Branch> findByIdAndSalonId(
            Long branchId,
            Long salonId
    );
}
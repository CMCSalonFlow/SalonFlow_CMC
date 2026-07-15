package com.example.salonflow.repository;

import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.BranchHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchHourRepository extends JpaRepository<BranchHour, Long> {

    List<BranchHour> findByBranch(Branch branch);

    java.util.Optional<BranchHour> findByBranchIdAndDayOfWeek(Long branchId, Integer dayOfWeek);

    void deleteByBranch(Branch branch);
}

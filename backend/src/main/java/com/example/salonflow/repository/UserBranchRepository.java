package com.example.salonflow.repository;


import com.example.salonflow.entity.UserBranch;
import com.example.salonflow.entity.UserBranchId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBranchRepository
        extends JpaRepository<UserBranch, UserBranchId> {

    List<UserBranch> findByUser_Id(Long userId);

    List<UserBranch> findByBranch_Id(Long branchId);

    boolean existsByUser_IdAndBranch_Id(
            Long userId,
            Long branchId
    );
    Optional<UserBranch> findByUser_IdAndBranch_Id(
                Long userId,
                Long branchId
        );
        void deleteByUser_IdAndBranch_Id(
                Long userId,
                Long branchId
        );

        void deleteByBranch_Id(Long branchId);
        @Query("""
        SELECT ub
        FROM UserBranch ub
        JOIN FETCH ub.user
        WHERE ub.branch.id = :branchId
        """)
        List<UserBranch> findAllUsersByBranchId(
                Long branchId
        );
}
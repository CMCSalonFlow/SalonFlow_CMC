package com.example.salonflow.repository;

import com.example.salonflow.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT AVG(b.ratingAverage) FROM Branch b WHERE b.salon.id = :salonId AND b.ratingCount > 0")
    Double calculateAverageBranchRatingBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT SUM(b.ratingCount) FROM Branch b WHERE b.salon.id = :salonId")
    Long sumRatingCountBySalonId(@Param("salonId") Long salonId);
}
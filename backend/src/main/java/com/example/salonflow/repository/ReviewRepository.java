package com.example.salonflow.repository;

import com.example.salonflow.entity.Review;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    boolean existsByBookingId(Long bookingId);

    Optional<Review> findByBookingId(Long bookingId);

    Page<Review> findBySalonId(Long salonId, Pageable pageable);

    Page<Review> findByBranchId(Long branchId, Pageable pageable);

    Page<Review> findBySentimentStatusOrderByCreatedAtAsc(ReviewSentimentStatus sentimentStatus, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.salon.id = :salonId")
    Double calculateAverageRatingBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.salon.id = :salonId")
    Long countBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.salon.id = :salonId GROUP BY r.rating")
    List<Object[]> countRatingDistributionBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.branch.id = :branchId")
    Double calculateAverageRatingByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.branch.id = :branchId")
    Long countByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.branch.id = :branchId GROUP BY r.rating")
    List<Object[]> countRatingDistributionByBranchId(@Param("branchId") Long branchId);
}

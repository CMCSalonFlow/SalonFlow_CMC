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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    boolean existsByBookingId(Long bookingId);

    Optional<Review> findByBookingId(Long bookingId);

    Page<Review> findBySalonId(Long salonId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE (r.salon.id = :salonId OR r.branch.salon.id = :salonId) AND (r.isHidden IS FALSE OR r.isHidden IS NULL)")
    Page<Review> findBySalonIdAndIsHiddenFalse(@Param("salonId") Long salonId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE (r.salon.id = :salonId OR r.branch.salon.id = :salonId) AND (:rating IS NULL OR r.rating = :rating) AND (r.isHidden IS FALSE OR r.isHidden IS NULL)")
    Page<Review> findBySalonIdAndRatingAndIsHiddenFalse(@Param("salonId") Long salonId, @Param("rating") Integer rating, Pageable pageable);

    Page<Review> findByBranchId(Long branchId, Pageable pageable);

    Page<Review> findByBranchIdAndIsHiddenFalse(Long branchId, Pageable pageable);

    Page<Review> findBySentimentStatusOrderByCreatedAtAsc(ReviewSentimentStatus sentimentStatus, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE (r.salon.id = :salonId OR r.branch.salon.id = :salonId) AND (r.isHidden IS FALSE OR r.isHidden IS NULL)")
    Double calculateAverageRatingBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT COUNT(r) FROM Review r WHERE (r.salon.id = :salonId OR r.branch.salon.id = :salonId) AND (r.isHidden IS FALSE OR r.isHidden IS NULL)")
    Long countBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE (r.salon.id = :salonId OR r.branch.salon.id = :salonId) AND (r.isHidden IS FALSE OR r.isHidden IS NULL) GROUP BY r.rating")
    List<Object[]> countRatingDistributionBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.branch.id = :branchId AND (r.isHidden IS FALSE OR r.isHidden IS NULL)")
    Double calculateAverageRatingByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.branch.id = :branchId AND (r.isHidden IS FALSE OR r.isHidden IS NULL)")
    Long countByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.branch.id = :branchId AND (r.isHidden IS FALSE OR r.isHidden IS NULL) GROUP BY r.rating")
    List<Object[]> countRatingDistributionByBranchId(@Param("branchId") Long branchId);

    // ---------- US-045: Dashboard Analytics ----------

    // 1) Trend rating theo tháng (line chart) - theo Salon
    @Query("SELECT FUNCTION('to_char', r.createdAt, 'YYYY-MM'), AVG(r.rating), COUNT(r) " +
            "FROM Review r WHERE r.salon.id = :salonId " +
            "AND (r.isHidden IS FALSE OR r.isHidden IS NULL) " +
            "AND r.createdAt >= :from AND r.createdAt < :to " +
            "GROUP BY FUNCTION('to_char', r.createdAt, 'YYYY-MM') " +
            "ORDER BY FUNCTION('to_char', r.createdAt, 'YYYY-MM')")
    List<Object[]> findMonthlyTrendBySalonId(@Param("salonId") Long salonId,
                                              @Param("from") Instant from,
                                              @Param("to") Instant to);

    // 1b) Trend rating theo tháng - theo Branch
    @Query("SELECT FUNCTION('to_char', r.createdAt, 'YYYY-MM'), AVG(r.rating), COUNT(r) " +
            "FROM Review r WHERE r.branch.id = :branchId " +
            "AND (r.isHidden IS FALSE OR r.isHidden IS NULL) " +
            "AND r.createdAt >= :from AND r.createdAt < :to " +
            "GROUP BY FUNCTION('to_char', r.createdAt, 'YYYY-MM') " +
            "ORDER BY FUNCTION('to_char', r.createdAt, 'YYYY-MM')")
    List<Object[]> findMonthlyTrendByBranchId(@Param("branchId") Long branchId,
                                               @Param("from") Instant from,
                                               @Param("to") Instant to);

    // 2) Top reviews tích cực / tiêu cực
    Page<Review> findBySalonIdAndIsHiddenFalseOrderByRatingDescCreatedAtDesc(Long salonId, Pageable pageable);

    Page<Review> findBySalonIdAndIsHiddenFalseOrderByRatingAscCreatedAtDesc(Long salonId, Pageable pageable);

    Page<Review> findByBranchIdAndIsHiddenFalseOrderByRatingDescCreatedAtDesc(Long branchId, Pageable pageable);

    Page<Review> findByBranchIdAndIsHiddenFalseOrderByRatingAscCreatedAtDesc(Long branchId, Pageable pageable);

    // 3) So sánh rating giữa các chi nhánh trong 1 salon
    @Query("SELECT r.branch.id, r.branch.name, AVG(r.rating), COUNT(r) " +
            "FROM Review r WHERE r.salon.id = :salonId " +
            "AND (r.isHidden IS FALSE OR r.isHidden IS NULL) " +
            "GROUP BY r.branch.id, r.branch.name " +
            "ORDER BY r.branch.name")
    List<Object[]> compareBranchesBySalonId(@Param("salonId") Long salonId);

    // 4) Export CSV - lấy toàn bộ review (không phân trang) theo salon/branch
    List<Review> findBySalonIdAndIsHiddenFalseOrderByCreatedAtDesc(Long salonId);

    List<Review> findByBranchIdAndIsHiddenFalseOrderByCreatedAtDesc(Long branchId);

    // ---------- US-045b: Word Cloud (batch job) ----------
    // 5) Lấy review đã có content, đã phân tích sentiment xong, trong 1 khoảng thời gian
    @Query("SELECT r FROM Review r WHERE r.sentimentStatus = com.example.salonflow.entity.enums.ReviewSentimentStatus.COMPLETED " +
            "AND (r.isHidden IS FALSE OR r.isHidden IS NULL) " +
            "AND r.content IS NOT NULL " +
            "AND r.createdAt >= :from AND r.createdAt < :to")
    List<Review> findCompletedReviewsForKeywordExtraction(@Param("from") Instant from, @Param("to") Instant to);
}
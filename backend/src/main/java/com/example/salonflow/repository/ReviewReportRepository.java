package com.example.salonflow.repository;

import com.example.salonflow.entity.ReviewReport;
import com.example.salonflow.entity.enums.ReviewReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    Page<ReviewReport> findByStatus(ReviewReportStatus status, Pageable pageable);

    boolean existsByReviewIdAndReporterIdAndStatus(Long reviewId, Long reporterId, ReviewReportStatus status);

    Optional<ReviewReport> findFirstByReviewIdAndReporterIdOrderByCreatedAtDesc(Long reviewId, Long reporterId);
}

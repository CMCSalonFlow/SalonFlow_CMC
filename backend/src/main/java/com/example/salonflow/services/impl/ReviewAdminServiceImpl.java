package com.example.salonflow.services.impl;

import com.example.salonflow.dto.review.ReviewAdminDetailResponse;
import com.example.salonflow.dto.review.ReviewAdminItemResponse;
import com.example.salonflow.dto.review.ReviewPageResponse;
import com.example.salonflow.dto.review.ReviewSentimentSummaryResponse;
import com.example.salonflow.entity.Review;
import com.example.salonflow.entity.enums.ReviewSentiment;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import com.example.salonflow.entity.enums.ReviewReportStatus;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.ReviewRepository;
import com.example.salonflow.repository.ReviewReportRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.entity.ReviewReport;
import com.example.salonflow.entity.User;
import com.example.salonflow.dto.review.ReviewReportResponse;
import com.example.salonflow.dto.review.ResolveReviewReportRequest;
import java.time.Instant;
import com.example.salonflow.services.service.ReviewAdminService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.security.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewAdminServiceImpl implements ReviewAdminService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final UserRepository userRepository;
    private final SalonRepository salonRepository;

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private Optional<Long> getScopedSalonId() {
        if (isAdmin()) {
            return Optional.empty(); // Super admin xem toàn hệ thống
        }
        Optional<Long> currentUserIdOpt = SecurityUtils.getCurrentUserIdOptional();
        if (currentUserIdOpt.isEmpty()) {
            return Optional.of(-1L);
        }
        return salonRepository.findFirstByOwnerId(currentUserIdOpt.get())
                .map(Salon::getId)
                .or(() -> Optional.of(-1L)); // User chưa tạo salon -> -1L
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPageResponse search(Long branchId, String sentiment, ReviewSentimentStatus status, String keyword, Pageable pageable) {
        Optional<Long> scopedSalonId = getScopedSalonId();
        if (scopedSalonId.isPresent() && scopedSalonId.get().equals(-1L)) {
            return ReviewPageResponse.builder()
                    .items(List.of())
                    .totalItems(0L)
                    .totalPages(0)
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build();
        }

        Long filterSalonId = scopedSalonId.orElse(null);
        Page<Review> page = reviewRepository.findAll(buildSpecification(filterSalonId, branchId, sentiment, status, keyword), pageable);

        return ReviewPageResponse.builder()
                .items(page.getContent().stream().map(this::toItemResponse).toList())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewAdminDetailResponse getById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review với id: " + reviewId));

        Optional<Long> scopedSalonId = getScopedSalonId();
        if (scopedSalonId.isPresent()) {
            Long ownerSalonId = scopedSalonId.get();
            boolean matchSalon = review.getSalon() != null && ownerSalonId.equals(review.getSalon().getId());
            boolean matchBranchSalon = review.getBranch() != null && review.getBranch().getSalon() != null
                    && ownerSalonId.equals(review.getBranch().getSalon().getId());
            if (!matchSalon && !matchBranchSalon) {
                throw new ResourceNotFoundException("Không tìm thấy review với id: " + reviewId);
            }
        }

        return toDetailResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSentimentSummaryResponse summary(Long branchId) {
        Optional<Long> scopedSalonId = getScopedSalonId();
        if (scopedSalonId.isPresent() && scopedSalonId.get().equals(-1L)) {
            return ReviewSentimentSummaryResponse.builder()
                    .total(0)
                    .pending(0)
                    .processing(0)
                    .completed(0)
                    .failed(0)
                    .positive(0)
                    .neutral(0)
                    .negative(0)
                    .build();
        }

        Long filterSalonId = scopedSalonId.orElse(null);
        Specification<Review> spec = buildSpecification(filterSalonId, branchId, null, null, null);
        java.util.List<Review> reviews = reviewRepository.findAll(spec);

        long pending = reviews.stream().filter(r -> r.getSentimentStatus() == ReviewSentimentStatus.PENDING).count();
        long processing = reviews.stream().filter(r -> r.getSentimentStatus() == ReviewSentimentStatus.PROCESSING).count();
        long completed = reviews.stream().filter(r -> r.getSentimentStatus() == ReviewSentimentStatus.COMPLETED).count();
        long failed = reviews.stream().filter(r -> r.getSentimentStatus() == ReviewSentimentStatus.FAILED).count();
        long positive = reviews.stream().filter(r -> r.getSentiment() == ReviewSentiment.POSITIVE).count();
        long neutral = reviews.stream().filter(r -> r.getSentiment() == ReviewSentiment.NEUTRAL).count();
        long negative = reviews.stream().filter(r -> r.getSentiment() == ReviewSentiment.NEGATIVE).count();

        return ReviewSentimentSummaryResponse.builder()
                .total(reviews.size())
                .pending(pending)
                .processing(processing)
                .completed(completed)
                .failed(failed)
                .positive(positive)
                .neutral(neutral)
                .negative(negative)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewReportResponse> getReviewReports(ReviewReportStatus status, Pageable pageable) {
        Page<ReviewReport> reports;
        if (status != null) {
            reports = reviewReportRepository.findByStatus(status, pageable);
        } else {
            reports = reviewReportRepository.findAll(pageable);
        }
        return reports.map(this::toReportResponse);
    }

    @Override
    @Transactional
    public ReviewReportResponse resolveReport(Long reportId, ResolveReviewReportRequest request, Long adminId) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy report với id: " + reportId));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        report.setStatus(ReviewReportStatus.RESOLVED);
        report.setAdminNotes(request.getAdminNotes());
        report.setResolvedBy(admin);
        report.setResolvedAt(Instant.now());

        report = reviewReportRepository.save(report);
        return toReportResponse(report);
    }

    private ReviewReportResponse toReportResponse(ReviewReport report) {
        return ReviewReportResponse.builder()
                .id(report.getId())
                .reviewId(report.getReview() != null ? report.getReview().getId() : null)
                .reviewRating(report.getReview() != null ? report.getReview().getRating() : null)
                .reviewComment(report.getReview() != null ? report.getReview().getComment() : null)
                .reviewAuthorName(report.getReview() != null && report.getReview().getUser() != null ? report.getReview().getUser().getFullName() : null)
                .reviewAuthorEmail(report.getReview() != null && report.getReview().getUser() != null ? report.getReview().getUser().getEmail() : null)
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterName(report.getReporter() != null ? report.getReporter().getFullName() : null)
                .reporterEmail(report.getReporter() != null ? report.getReporter().getEmail() : null)
                .reason(report.getReason())
                .status(report.getStatus())
                .adminNotes(report.getAdminNotes())
                .resolvedById(report.getResolvedBy() != null ? report.getResolvedBy().getId() : null)
                .resolvedByName(report.getResolvedBy() != null ? report.getResolvedBy().getFullName() : null)
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private Specification<Review> buildSpecification(Long salonId, Long branchId, String sentiment, ReviewSentimentStatus status, String keyword) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();

            if (salonId != null) {
                Predicate salonMatch = cb.equal(root.get("salon").get("id"), salonId);
                Predicate branchSalonMatch = cb.equal(root.get("branch").get("salon").get("id"), salonId);
                predicates.add(cb.or(salonMatch, branchSalonMatch));
            }

            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }

            if (sentiment != null && !sentiment.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("sentiment").as(String.class)), sentiment.trim().toLowerCase(Locale.ROOT)));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("sentimentStatus"), status));
            }

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("content")), like),
                        cb.like(cb.lower(root.get("user").get("fullName")), like),
                        cb.like(cb.lower(root.get("user").get("email")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ReviewAdminItemResponse toItemResponse(Review review) {
        return ReviewAdminItemResponse.builder()
                .id(review.getId())
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(review.getUser() != null ? review.getUser().getFullName() : null)
                .branchId(review.getBranch() != null ? review.getBranch().getId() : null)
                .branchName(review.getBranch() != null ? review.getBranch().getName() : null)
                .rating(review.getRating())
                .sentiment(review.getSentiment() != null ? review.getSentiment().name() : null)
                .sentimentConfidence(review.getSentimentConfidence())
                .sentimentStatus(review.getSentimentStatus() != null ? review.getSentimentStatus().name() : null)
                .title(review.getTitle())
                .content(review.getContent())
                .comment(review.getComment())
                .ownerReply(review.getOwnerReply())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private ReviewAdminDetailResponse toDetailResponse(Review review) {
        ReviewAdminItemResponse item = toItemResponse(review);
        ReviewAdminDetailResponse detail = new ReviewAdminDetailResponse();
        detail.setId(item.getId());
        detail.setUserId(item.getUserId());
        detail.setUserName(item.getUserName());
        detail.setBranchId(item.getBranchId());
        detail.setBranchName(item.getBranchName());
        detail.setRating(item.getRating());
        detail.setSentiment(item.getSentiment());
        detail.setSentimentConfidence(item.getSentimentConfidence());
        detail.setSentimentStatus(item.getSentimentStatus());
        detail.setTitle(item.getTitle());
        detail.setContent(item.getContent());
        detail.setComment(item.getComment());
        detail.setOwnerReply(item.getOwnerReply());
        detail.setCreatedAt(item.getCreatedAt());
        return detail;
    }
}

package com.example.salonflow.services.impl;

import com.example.salonflow.dto.review.ReviewAdminDetailResponse;
import com.example.salonflow.dto.review.ReviewAdminItemResponse;
import com.example.salonflow.dto.review.ReviewPageResponse;
import com.example.salonflow.dto.review.ReviewSentimentSummaryResponse;
import com.example.salonflow.entity.Review;
import com.example.salonflow.entity.enums.ReviewSentiment;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.ReviewRepository;
import com.example.salonflow.services.service.ReviewAdminService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReviewAdminServiceImpl implements ReviewAdminService {

    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public ReviewPageResponse search(Long branchId, String sentiment, ReviewSentimentStatus status, String keyword, Pageable pageable) {
        Page<Review> page = reviewRepository.findAll(buildSpecification(branchId, sentiment, status, keyword), pageable);

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
        return toDetailResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSentimentSummaryResponse summary(Long branchId) {
        Specification<Review> spec = buildSpecification(branchId, null, null, null);
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

    private Specification<Review> buildSpecification(Long branchId, String sentiment, ReviewSentimentStatus status, String keyword) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();

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
                .bookingId(review.getBooking() != null ? review.getBooking().getId() : null)
                .branchId(review.getBranch() != null ? review.getBranch().getId() : null)
                .branchName(review.getBranch() != null ? review.getBranch().getName() : null)
                .staffId(review.getStaff() != null ? review.getStaff().getId() : null)
                .staffName(review.getStaff() != null ? review.getStaff().getName() : null)
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .sentiment(review.getSentiment() != null ? review.getSentiment().name() : null)
                .sentimentConfidence(review.getSentimentConfidence())
                .sentimentStatus(review.getSentimentStatus() != null ? review.getSentimentStatus().name() : null)
                .sentimentProvider(review.getSentimentProvider())
                .sentimentAnalyzedAt(review.getSentimentAnalyzedAt())
                .sentimentError(review.getSentimentError())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .sentimentBadgeColor(resolveBadgeColor(review))
                .build();
    }

    private ReviewAdminDetailResponse toDetailResponse(Review review) {
        ReviewAdminItemResponse item = toItemResponse(review);
        ReviewAdminDetailResponse detail = new ReviewAdminDetailResponse();
        detail.setId(item.getId());
        detail.setUserId(item.getUserId());
        detail.setUserName(item.getUserName());
        detail.setBookingId(item.getBookingId());
        detail.setBranchId(item.getBranchId());
        detail.setBranchName(item.getBranchName());
        detail.setStaffId(item.getStaffId());
        detail.setStaffName(item.getStaffName());
        detail.setRating(item.getRating());
        detail.setTitle(item.getTitle());
        detail.setContent(item.getContent());
        detail.setSentiment(item.getSentiment());
        detail.setSentimentConfidence(item.getSentimentConfidence());
        detail.setSentimentStatus(item.getSentimentStatus());
        detail.setSentimentProvider(item.getSentimentProvider());
        detail.setSentimentAnalyzedAt(item.getSentimentAnalyzedAt());
        detail.setSentimentError(item.getSentimentError());
        detail.setCreatedAt(item.getCreatedAt());
        detail.setUpdatedAt(item.getUpdatedAt());
        detail.setSentimentBadgeColor(item.getSentimentBadgeColor());
        return detail;
    }

    private String resolveBadgeColor(Review review) {
        if (review.getSentiment() == null) {
            return "gray";
        }
        return switch (review.getSentiment()) {
            case POSITIVE -> "green";
            case NEUTRAL -> "yellow";
            case NEGATIVE -> "red";
        };
    }
}

package com.example.salonflow.services.impl;

import com.example.salonflow.dto.review.BranchRatingSummaryResponse;
import com.example.salonflow.dto.review.CreateReviewRequest;
import com.example.salonflow.dto.review.ReviewResponse;
import com.example.salonflow.dto.review.SalonRatingSummaryResponse;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.exception.BusinessAccessDeniedException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ReviewRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.services.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final SalonRepository salonRepository;
    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(Long bookingId, CreateReviewRequest request, Long currentUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch với ID: " + bookingId));

        // 1. Middleware / Service check: booking.user_id = current_user.id
        if (!booking.getCustomer().getId().equals(currentUserId)) {
            throw new BusinessAccessDeniedException("Bạn không có quyền đánh giá đơn đặt lịch này.");
        }

        // 2. Middleware / Service check: booking.status = COMPLETED
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessAccessDeniedException("Chỉ cho phép đánh giá sau khi dịch vụ đã hoàn thành.");
        }

        // 3. Check if already reviewed
        if (booking.getReviewedAt() != null || reviewRepository.existsByBookingId(bookingId)) {
            throw new BusinessAccessDeniedException("Lịch hẹn này đã được đánh giá trước đó.");
        }

        // 4. Check photos limit (max 5)
        if (request.getPhotos() != null && request.getPhotos().size() > 5) {
            throw new IllegalArgumentException("Tối đa chỉ được tải lên 5 ảnh đánh giá.");
        }

        Branch branch = booking.getBranch();
        Salon salon = branch.getSalon();

        Review review = Review.builder()
                .booking(booking)
                .user(booking.getCustomer())
                .salon(salon)
                .branch(branch)
                .rating(request.getRating())
                .content(request.getComment())
                .build();

        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            List<ReviewPhoto> photoEntities = request.getPhotos().stream()
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .map(url -> ReviewPhoto.builder()
                            .review(review)
                            .photoUrl(url.trim())
                            .build())
                    .collect(Collectors.toList());
            review.setPhotos(photoEntities);
        }

        Review savedReview = reviewRepository.save(review);

        // Update booking.reviewed_at
        booking.setReviewedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // 1. Recalculate average rating & rating count of branch
        updateBranchRatingSummary(branch.getId());

        // 2. Recalculate average rating & rating count of salon (based on branch averages)
        updateSalonRatingSummary(salon.getId());

        return mapToResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByBookingId(Long bookingId) {
        Review review = reviewRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có đánh giá nào cho đơn đặt lịch này."));
        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsBySalonId(Long salonId, Pageable pageable) {
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException("Không tìm thấy Salon với ID: " + salonId);
        }
        return reviewRepository.findBySalonId(salonId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByBranchId(Long branchId, Pageable pageable) {
        return reviewRepository.findByBranchId(branchId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SalonRatingSummaryResponse getSalonReviewSummary(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Salon với ID: " + salonId));

        Double avg = branchRepository.calculateAverageBranchRatingBySalonId(salonId);
        Long count = branchRepository.sumRatingCountBySalonId(salonId);

        BigDecimal averageRating = avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        long totalReviews = count != null ? count : 0L;

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        List<Object[]> rawDistribution = reviewRepository.countRatingDistributionBySalonId(salonId);
        for (Object[] row : rawDistribution) {
            Integer star = (Integer) row[0];
            Long cnt = (Long) row[1];
            if (star != null && star >= 1 && star <= 5) {
                distribution.put(star, cnt);
            }
        }

        return SalonRatingSummaryResponse.builder()
                .salonId(salonId)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .ratingDistribution(distribution)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchRatingSummaryResponse getBranchReviewSummary(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy Chi nhánh với ID: " + branchId);
        }

        Double avg = reviewRepository.calculateAverageRatingByBranchId(branchId);
        Long count = reviewRepository.countByBranchId(branchId);

        BigDecimal averageRating = avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        long totalReviews = count != null ? count : 0L;

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        List<Object[]> rawDistribution = reviewRepository.countRatingDistributionByBranchId(branchId);
        for (Object[] row : rawDistribution) {
            Integer star = (Integer) row[0];
            Long cnt = (Long) row[1];
            if (star != null && star >= 1 && star <= 5) {
                distribution.put(star, cnt);
            }
        }

        return BranchRatingSummaryResponse.builder()
                .branchId(branchId)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .ratingDistribution(distribution)
                .build();
    }

    private void updateBranchRatingSummary(Long branchId) {
        Branch branch = branchRepository.findById(branchId).orElse(null);
        if (branch == null) return;

        Double avg = reviewRepository.calculateAverageRatingByBranchId(branchId);
        Long count = reviewRepository.countByBranchId(branchId);

        BigDecimal averageRating = avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        int totalReviews = count != null ? count.intValue() : 0;

        branch.setRatingAverage(averageRating);
        branch.setRatingCount(totalReviews);
        branchRepository.save(branch);
    }

    private void updateSalonRatingSummary(Long salonId) {
        Salon salon = salonRepository.findById(salonId).orElse(null);
        if (salon == null) return;

        Double avg = branchRepository.calculateAverageBranchRatingBySalonId(salonId);
        Long countSum = branchRepository.sumRatingCountBySalonId(salonId);

        BigDecimal averageRating = avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        int totalReviews = countSum != null ? countSum.intValue() : 0;

        salon.setRatingAverage(averageRating);
        salon.setRatingCount(totalReviews);
        salonRepository.save(salon);
    }

    private ReviewResponse mapToResponse(Review review) {
        User customer = review.getCustomer();

        List<String> photoUrls = review.getPhotos() != null ?
                review.getPhotos().stream()
                        .map(ReviewPhoto::getPhotoUrl)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .customerId(customer.getId())
                .customerName(customer.getFullName())
                .customerAvatar(customer.getAvatarUrl())
                .salonId(review.getSalon().getId())
                .branchId(review.getBranch().getId())
                .branchName(review.getBranch().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .photos(photoUrls)
                .createdAt(review.getCreatedAt())
                .build();
    }
}

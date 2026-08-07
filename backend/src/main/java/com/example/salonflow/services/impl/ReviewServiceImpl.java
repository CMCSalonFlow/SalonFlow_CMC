package com.example.salonflow.services.impl;

import com.example.salonflow.dto.review.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.entity.enums.ReviewReportStatus;
import com.example.salonflow.exception.BusinessAccessDeniedException;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.EmailService;
import com.example.salonflow.services.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final BookingRepository bookingRepository;
    private final SalonRepository salonRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public ReviewResponse createReview(Long bookingId, CreateReviewRequest request, Long currentUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch với ID: " + bookingId));

        if (!booking.getCustomer().getId().equals(currentUserId)) {
            throw new BusinessAccessDeniedException("Bạn không có quyền đánh giá đơn đặt lịch này.");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException("Chỉ cho phép đánh giá sau khi dịch vụ đã hoàn thành.");
        }

        if (booking.getReviewedAt() != null || reviewRepository.existsByBookingId(bookingId)) {
            throw new BusinessException("Lịch hẹn này đã được đánh giá trước đó.");
        }

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
                .isHidden(false)
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

        booking.setReviewedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        updateBranchRatingSummary(branch.getId());
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
    public Page<ReviewResponse> getReviewsBySalonId(Long salonId, Integer rating, Pageable pageable) {
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException("Không tìm thấy Salon với ID: " + salonId);
        }
        return reviewRepository.findBySalonIdAndRatingAndIsHiddenFalse(salonId, rating, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByBranchId(Long branchId, Pageable pageable) {
        return reviewRepository.findByBranchIdAndIsHiddenFalse(branchId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SalonRatingSummaryResponse getSalonReviewSummary(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Salon với ID: " + salonId));

        Double avg = reviewRepository.calculateAverageRatingBySalonId(salonId);
        Long count = reviewRepository.countBySalonId(salonId);

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

    @Override
    @Transactional
    public ReviewResponse replyReview(Long reviewId, OwnerReplyReviewRequest request, Long currentUserId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đánh giá với ID: " + reviewId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));

        // Kiểm tra xem currentUserId có phải là Owner của Salon này không
        if (review.getSalon() != null && review.getSalon().getOwner() != null) {
            if (!review.getSalon().getOwner().getId().equals(currentUserId)) {
                throw new BusinessAccessDeniedException("Bạn không có quyền phản hồi bài đánh giá của Salon này.");
            }
        }

        // Quy tắc: 1 reply per review
        if (review.getOwnerReply() != null && !review.getOwnerReply().trim().isEmpty()) {
            throw new BusinessException("Bài đánh giá này đã được Salon phản hồi trước đó. Mỗi đánh giá chỉ được phản hồi duy nhất 1 lần.");
        }

        review.setOwnerReply(request.getReplyContent().trim());
        review.setOwnerRepliedAt(Instant.now());
        review = reviewRepository.save(review);

        return mapToResponse(review);
    }

    @Override
    @Transactional
    public ReviewReportResponse reportReview(Long reviewId, ReportReviewRequest request, Long currentUserId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đánh giá với ID: " + reviewId));

        User reporter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));

        // Kiểm tra trùng lặp report đang chờ duyệt
        boolean hasPendingReport = reviewReportRepository.existsByReviewIdAndReporterIdAndStatus(
                reviewId, currentUserId, ReviewReportStatus.PENDING
        );
        if (hasPendingReport) {
            throw new BusinessException("Bạn đã gửi báo cáo vi phạm cho đánh giá này và đang đợi Admin xét duyệt.");
        }

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporter(reporter)
                .reason(request.getReason().trim())
                .status(ReviewReportStatus.PENDING)
                .build();

        report = reviewReportRepository.save(report);

        return mapToReportResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewReportResponse> getReviewReports(ReviewReportStatus status, Pageable pageable) {
        ReviewReportStatus filterStatus = status != null ? status : ReviewReportStatus.PENDING;
        return reviewReportRepository.findByStatus(filterStatus, pageable)
                .map(this::mapToReportResponse);
    }

    @Override
    @Transactional
    public ReviewReportResponse approveReport(Long reportId, ResolveReviewReportRequest request, Long adminUserId) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu báo cáo với ID: " + reportId));

        if (report.getStatus() != ReviewReportStatus.PENDING) {
            throw new BusinessException("Báo cáo vi phạm này đã được xử lý trước đó.");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Admin với ID: " + adminUserId));

        String notes = request != null && request.getAdminNotes() != null ? request.getAdminNotes().trim() : "Đã xác minh vi phạm tiêu chuẩn cộng đồng.";

        report.setStatus(ReviewReportStatus.APPROVED);
        report.setAdminNotes(notes);
        report.setResolvedBy(admin);
        report.setResolvedAt(Instant.now());
        report = reviewReportRepository.save(report);

        // Ẩn review khỏi public nhưng giữ trong DB
        Review review = report.getReview();
        review.setIsHidden(true);
        review.setHiddenAt(Instant.now());
        review.setHiddenReason(notes);
        reviewRepository.save(review);

        // Cập nhật lại điểm đánh giá trung bình
        if (review.getBranch() != null) {
            updateBranchRatingSummary(review.getBranch().getId());
        }
        if (review.getSalon() != null) {
            updateSalonRatingSummary(review.getSalon().getId());
        }

        // Gửi email thông báo kết quả cho cả 2 bên
        sendReportApprovalEmails(report, review, notes);

        return mapToReportResponse(report);
    }

    @Override
    @Transactional
    public ReviewReportResponse rejectReport(Long reportId, ResolveReviewReportRequest request, Long adminUserId) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu báo cáo với ID: " + reportId));

        if (report.getStatus() != ReviewReportStatus.PENDING) {
            throw new BusinessException("Báo cáo vi phạm này đã được xử lý trước đó.");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Admin với ID: " + adminUserId));

        String notes = request != null && request.getAdminNotes() != null ? request.getAdminNotes().trim() : "Đánh giá không vi phạm quy định cộng đồng.";

        report.setStatus(ReviewReportStatus.REJECTED);
        report.setAdminNotes(notes);
        report.setResolvedBy(admin);
        report.setResolvedAt(Instant.now());
        report = reviewReportRepository.save(report);

        // Gửi email thông báo kết quả từ chối báo cáo cho Người báo cáo
        sendReportRejectionEmails(report, notes);

        return mapToReportResponse(report);
    }

    private void sendReportApprovalEmails(ReviewReport report, Review review, String adminNotes) {
        try {
            User reporter = report.getReporter();
            User customer = review.getCustomer();

            // Email 1: Gửi cho Người báo cáo (Salon Owner / User)
            if (reporter != null && reporter.getEmail() != null) {
                String subject = "[SalonFlow] Báo cáo vi phạm đánh giá #" + review.getId() + " đã được chấp nhận";
                String body = "Kính gửi " + reporter.getFullName() + ",\n\n" +
                        "Báo cáo vi phạm cho đánh giá #" + review.getId() + " của bạn đã được Quản trị viên chấp nhận.\n" +
                        "Bài đánh giá này đã được ẩn khỏi danh sách hiển thị công khai trên hệ thống.\n\n" +
                        "Ghi chú từ Admin: " + adminNotes + "\n\n" +
                        "Cảm ơn bạn đã đóng góp duy trì môi trường minh bạch cho SalonFlow!";
                emailService.sendNotificationEmail(reporter.getEmail(), subject, body);
            }

            // Email 2: Gửi cho Người viết đánh giá (Khách hàng)
            if (customer != null && customer.getEmail() != null) {
                String subject = "[SalonFlow Thông báo] Đánh giá của bạn đã bị ẩn do vi phạm tiêu chuẩn cộng đồng";
                String body = "Kính gửi " + customer.getFullName() + ",\n\n" +
                        "Bài đánh giá của bạn cho đơn đặt lịch tại " + (review.getSalon() != null ? review.getSalon().getName() : "Salon") +
                        " đã bị ẩn theo quy định quản lý nội dung của SalonFlow.\n\n" +
                        "Lý do: " + adminNotes + "\n\n" +
                        "Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ bộ phận hỗ trợ khách hàng SalonFlow.";
                emailService.sendNotificationEmail(customer.getEmail(), subject, body);
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thông báo kết quả duyệt report review ID: {}", review.getId(), e);
        }
    }

    private void sendReportRejectionEmails(ReviewReport report, String adminNotes) {
        try {
            User reporter = report.getReporter();
            Review review = report.getReview();

            if (reporter != null && reporter.getEmail() != null) {
                String subject = "[SalonFlow] Kết quả xử lý báo cáo vi phạm đánh giá #" + review.getId();
                String body = "Kính gửi " + reporter.getFullName() + ",\n\n" +
                        "Yêu cầu báo cáo vi phạm cho đánh giá #" + review.getId() + " đã được Quản trị viên xem xét.\n" +
                        "Kết quả: Đánh giá này tuân thủ đúng quy chuẩn nội dung nên sẽ tiếp tục được giữ lại trên hệ thống.\n\n" +
                        "Ghi chú từ Admin: " + adminNotes + "\n\n" +
                        "Trân trọng,\nĐội ngũ SalonFlow.";
                emailService.sendNotificationEmail(reporter.getEmail(), subject, body);
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thông báo từ chối report review ID: {}", report.getReview().getId(), e);
        }
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

        Double avg = reviewRepository.calculateAverageRatingBySalonId(salonId);
        Long countSum = reviewRepository.countBySalonId(salonId);

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
                        .map(photo -> {
                            if (photo.getMedia() != null && photo.getMedia().getUrl() != null && !photo.getMedia().getUrl().isBlank()) {
                                return photo.getMedia().getUrl();
                            }
                            return photo.getPhotoUrl();
                        })
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return ReviewResponse.builder()
                .id(review.getId())
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getFullName() : null)
                .customerAvatar(customer != null ? customer.getAvatarUrl() : null)
                .salonId(review.getSalon() != null ? review.getSalon().getId() : null)
                .branchId(review.getBranch() != null ? review.getBranch().getId() : null)
                .branchName(review.getBranch() != null ? review.getBranch().getName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .photos(photoUrls)
                .ownerReply(review.getOwnerReply())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private ReviewReportResponse mapToReportResponse(ReviewReport report) {
        Review review = report.getReview();
        User reporter = report.getReporter();
        User author = review != null ? review.getCustomer() : null;
        User resolver = report.getResolvedBy();

        return ReviewReportResponse.builder()
                .id(report.getId())
                .reviewId(review != null ? review.getId() : null)
                .reviewRating(review != null ? review.getRating() : null)
                .reviewComment(review != null ? review.getComment() : null)
                .reviewAuthorName(author != null ? author.getFullName() : null)
                .reviewAuthorEmail(author != null ? author.getEmail() : null)
                .reporterId(reporter != null ? reporter.getId() : null)
                .reporterName(reporter != null ? reporter.getFullName() : null)
                .reporterEmail(reporter != null ? reporter.getEmail() : null)
                .reason(report.getReason())
                .status(report.getStatus())
                .adminNotes(report.getAdminNotes())
                .resolvedById(resolver != null ? resolver.getId() : null)
                .resolvedByName(resolver != null ? resolver.getFullName() : null)
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}

package com.example.salonflow.services.impl;

import com.example.salonflow.dto.reviewanalytics.*;
import com.example.salonflow.entity.Review;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ReviewRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.ReviewAnalyticsService;
import com.example.salonflow.validation.BranchOwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewAnalyticsServiceImpl implements ReviewAnalyticsService {

    private final ReviewRepository reviewRepository;
    private final SalonRepository salonRepository;
    private final BranchRepository branchRepository;
    private final BranchOwnershipValidator branchOwnershipValidator;

    private static final DateTimeFormatter CSV_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    // ---------------------------------------------------------------
    // 1) Trend theo tháng
    // ---------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public RatingTrendResponse getRatingTrend(Long salonId, Long branchId, YearMonth fromMonth, YearMonth toMonth) {
        validateScope(salonId, branchId);

        YearMonth from = fromMonth != null ? fromMonth : YearMonth.now().minusMonths(11);
        YearMonth to = toMonth != null ? toMonth : YearMonth.now();

        if (from.isAfter(to)) {
            throw new BadRequestException("fromMonth không được sau toMonth.");
        }

        Instant fromInstant = from.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        // toMonth là tháng cuối cùng CẦN BAO GỒM -> lấy đến đầu tháng kế tiếp
        Instant toInstant = to.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> rows;
        if (branchId != null) {
            rows = reviewRepository.findMonthlyTrendByBranchId(branchId, fromInstant, toInstant);
        } else {
            rows = reviewRepository.findMonthlyTrendBySalonId(salonId, fromInstant, toInstant);
        }

        Map<String, RatingTrendPointResponse> byMonth = new HashMap<>();
        for (Object[] row : rows) {
            String month = (String) row[0];
            Double avg = (Double) row[1];
            Long count = (Long) row[2];
            byMonth.put(month, RatingTrendPointResponse.builder()
                    .month(month)
                    .averageRating(avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                    .totalReviews(count != null ? count : 0L)
                    .build());
        }

        // Điền đủ các tháng trong khoảng, kể cả tháng không có review nào (để chart không bị đứt đoạn)
        List<RatingTrendPointResponse> points = new ArrayList<>();
        YearMonth cursor = from;
        while (!cursor.isAfter(to)) {
            String key = cursor.toString(); // "YYYY-MM"
            points.add(byMonth.getOrDefault(key, RatingTrendPointResponse.builder()
                    .month(key)
                    .averageRating(BigDecimal.ZERO)
                    .totalReviews(0L)
                    .build()));
            cursor = cursor.plusMonths(1);
        }

        return RatingTrendResponse.builder()
                .salonId(salonId)
                .branchId(branchId)
                .fromMonth(from.toString())
                .toMonth(to.toString())
                .points(points)
                .build();
    }

    // ---------------------------------------------------------------
    // 2) Top reviews
    // ---------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public TopReviewListResponse getTopReviews(Long salonId, Long branchId, int limit) {
        validateScope(salonId, branchId);
        int safeLimit = limit <= 0 ? 5 : Math.min(limit, 50);
        PageRequest page = PageRequest.of(0, safeLimit);

        List<Review> positives;
        List<Review> negatives;
        if (branchId != null) {
            positives = reviewRepository.findByBranchIdAndIsHiddenFalseOrderByRatingDescCreatedAtDesc(branchId, page).getContent();
            negatives = reviewRepository.findByBranchIdAndIsHiddenFalseOrderByRatingAscCreatedAtDesc(branchId, page).getContent();
        } else {
            positives = reviewRepository.findBySalonIdAndIsHiddenFalseOrderByRatingDescCreatedAtDesc(salonId, page).getContent();
            negatives = reviewRepository.findBySalonIdAndIsHiddenFalseOrderByRatingAscCreatedAtDesc(salonId, page).getContent();
        }

        return TopReviewListResponse.builder()
                .topPositive(positives.stream().map(this::mapToTopReviewResponse).toList())
                .topNegative(negatives.stream().map(this::mapToTopReviewResponse).toList())
                .build();
    }

    private TopReviewResponse mapToTopReviewResponse(Review review) {
        return TopReviewResponse.builder()
                .id(review.getId())
                .customerName(review.getCustomer() != null ? review.getCustomer().getFullName() : null)
                .customerAvatar(review.getCustomer() != null ? review.getCustomer().getAvatarUrl() : null)
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .sentiment(review.getSentiment() != null ? review.getSentiment().name() : null)
                .branchName(review.getBranch() != null ? review.getBranch().getName() : null)
                .createdAt(review.getCreatedAt())
                .build();
    }

    // ---------------------------------------------------------------
    // 3) So sánh chi nhánh
    // ---------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public BranchComparisonResponse compareBranches(Long salonId) {
        if (salonId == null) {
            throw new BadRequestException("salonId là bắt buộc để so sánh chi nhánh.");
        }
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException("Không tìm thấy Salon với ID: " + salonId);
        }
        if (!isAdmin()) {
            validateOwnerOwnsSalon(salonId);
        }

        List<Object[]> rows = reviewRepository.compareBranchesBySalonId(salonId);

        List<BranchComparisonItemResponse> branches = new ArrayList<>();
        for (Object[] row : rows) {
            Long branchId = (Long) row[0];
            String branchName = (String) row[1];
            Double avg = (Double) row[2];
            Long count = (Long) row[3];

            Map<Integer, Long> distribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                distribution.put(i, 0L);
            }
            List<Object[]> distRows = reviewRepository.countRatingDistributionByBranchId(branchId);
            for (Object[] d : distRows) {
                Integer star = (Integer) d[0];
                Long cnt = (Long) d[1];
                if (star != null && star >= 1 && star <= 5) {
                    distribution.put(star, cnt);
                }
            }

            branches.add(BranchComparisonItemResponse.builder()
                    .branchId(branchId)
                    .branchName(branchName)
                    .averageRating(avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                    .totalReviews(count != null ? count : 0L)
                    .ratingDistribution(distribution)
                    .build());
        }

        return BranchComparisonResponse.builder()
                .salonId(salonId)
                .branches(branches)
                .build();
    }

    // ---------------------------------------------------------------
    // 4) Export CSV
    // ---------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public byte[] exportReviewsCsv(Long salonId, Long branchId) {
        validateScope(salonId, branchId);

        List<Review> reviews = branchId != null
                ? reviewRepository.findByBranchIdAndIsHiddenFalseOrderByCreatedAtDesc(branchId)
                : reviewRepository.findBySalonIdAndIsHiddenFalseOrderByCreatedAtDesc(salonId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            // BOM để Excel nhận đúng UTF-8 tiếng Việt
            writer.write('\uFEFF');
            writer.write("ID,Khach hang,Chi nhanh,So sao,Sentiment,Tieu de,Noi dung,Phan hoi cua Salon,Ngay tao\n");
            for (Review r : reviews) {
                writer.write(String.join(",",
                        csv(r.getId()),
                        csv(r.getCustomer() != null ? r.getCustomer().getFullName() : ""),
                        csv(r.getBranch() != null ? r.getBranch().getName() : ""),
                        csv(r.getRating()),
                        csv(r.getSentiment() != null ? r.getSentiment().name() : ""),
                        csv(r.getTitle()),
                        csv(r.getComment()),
                        csv(r.getOwnerReply()),
                        csv(r.getCreatedAt() != null ? CSV_DATE_FORMAT.format(r.getCreatedAt()) : "")
                ));
                writer.write("\n");
            }
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Lỗi khi tạo file CSV export review.", e);
        }
        return baos.toByteArray();
    }

    private String csv(Object value) {
        if (value == null) return "";
        String s = value.toString().replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    // ---------------------------------------------------------------
    private void validateScope(Long salonId, Long branchId) {
        if (salonId == null && branchId == null) {
            throw new BadRequestException("Cần truyền salonId hoặc branchId.");
        }
        if (branchId != null && !branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy Chi nhánh với ID: " + branchId);
        }
        if (branchId == null && salonId != null && !salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException("Không tìm thấy Salon với ID: " + salonId);
        }

        if (isAdmin()) {
            return; // Admin/Super Admin xem toàn hệ thống, không giới hạn.
        }

        // Salon Owner: chỉ được xem đúng salon/branch mình sở hữu.
        // branchId ưu tiên hơn salonId (khớp với logic chọn scope ở các hàm trên).
        if (branchId != null) {
            // Ném BusinessAccessDeniedException nếu branch không thuộc salon của owner hiện tại.
            branchOwnershipValidator.validateOwnerBranch(branchId);
        } else {
            validateOwnerOwnsSalon(salonId);
        }
    }

    /** Chỉ salon do chính owner hiện tại sở hữu mới được xem (KHÔNG mở rộng qua UserBranch). */
    private void validateOwnerOwnsSalon(Long salonId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isOwnSalon = salonRepository.findFirstByOwnerId(currentUserId)
                .map(salon -> salon.getId().equals(salonId))
                .orElse(false);
        if (!isOwnSalon) {
            throw new com.example.salonflow.exception.BusinessAccessDeniedException(
                    "Bạn không có quyền xem dữ liệu của salon này.");
        }
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}

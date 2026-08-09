package com.example.salonflow.services.impl;

import com.example.salonflow.dto.reviewanalytics.KeywordFrequencyResponse;
import com.example.salonflow.dto.reviewanalytics.WordCloudResponse;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Review;
import com.example.salonflow.entity.ReviewKeyword;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.BusinessAccessDeniedException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ReviewKeywordRepository;
import com.example.salonflow.repository.ReviewRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.ReviewKeywordService;
import com.example.salonflow.util.VietnameseKeywordExtractor;
import com.example.salonflow.validation.BranchOwnershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewKeywordServiceImpl implements ReviewKeywordService {

    private final ReviewRepository reviewRepository;
    private final ReviewKeywordRepository reviewKeywordRepository;
    private final SalonRepository salonRepository;
    private final BranchRepository branchRepository;
    private final BranchOwnershipValidator branchOwnershipValidator;

    /**
     * Chạy mỗi ngày lúc 2h sáng — tính lại từ khoá cho THÁNG HIỆN TẠI.
     * Đây chính là "batch job/scheduler tính sẵn tần suất từ khoá, KHÔNG tính real-time"
     * theo đúng quyết định nghiệp vụ đã chốt trong US-045.
     *
     * ⚠️ Cron chạy theo giờ hệ thống JVM (mặc định UTC nếu server không set timezone).
     * Nếu muốn chạy đúng 2h sáng giờ Việt Nam, cần thêm zone="Asia/Ho_Chi_Minh"
     * hoặc cấu hình timezone JVM — kiểm tra lại với DevOps/leader trước khi deploy.
     *
     * ⚠️ @Transactional BẮT BUỘC phải đặt ở đây (method được @Scheduled gọi trực tiếp
     * từ bên ngoài class) — không đặt ở recomputeKeywordsForMonth() vì đó là self-invocation
     * (gọi nội bộ trong cùng class), Spring AOP sẽ bỏ qua @Transactional trong trường hợp đó,
     * gây lỗi LazyInitializationException khi lazy-load branch.getSalon().
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void scheduledRecomputeCurrentMonth() {
        log.info("[WordCloud] Bắt đầu batch job tính từ khoá cho tháng hiện tại");
        try {
            recomputeKeywordsForMonth(YearMonth.now());
            log.info("[WordCloud] Hoàn tất batch job tính từ khoá");
        } catch (Exception e) {
            log.error("[WordCloud] Lỗi khi chạy batch job tính từ khoá", e);
        }
    }

    @Override
    @Transactional
    public void recomputeKeywordsForMonth(YearMonth yearMonth) {
        Instant from = yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        String yearMonthStr = yearMonth.toString(); // "YYYY-MM"

        List<Review> reviews = reviewRepository.findCompletedReviewsForKeywordExtraction(from, to);

        // Gom review theo branch
        Map<Long, List<Review>> reviewsByBranch = reviews.stream()
                .filter(r -> r.getBranch() != null)
                .collect(Collectors.groupingBy(r -> r.getBranch().getId()));

        for (Map.Entry<Long, List<Review>> entry : reviewsByBranch.entrySet()) {
            Long branchId = entry.getKey();
            List<Review> branchReviews = entry.getValue();

            Map<String, Integer> freq = VietnameseKeywordExtractor.mergeFrequencies(
                    branchReviews.stream()
                            .map(r -> VietnameseKeywordExtractor.extractFrequency(r.getContent()))
                            .toList()
            );

            if (freq.isEmpty()) {
                continue;
            }

            Branch branch = branchReviews.get(0).getBranch();
            Salon salon = branchReviews.get(0).getSalon();
            if (salon == null) {
                // Phòng trường hợp review cũ chưa gán salon trực tiếp - lấy qua branch
                salon = branch.getSalon();
            }

            // Xoá dữ liệu cũ của branch+tháng này trước khi ghi lại (tránh giữ từ khoá đã lỗi thời
            // nếu review bị sửa/xoá/ẩn giữa các lần chạy job)
            reviewKeywordRepository.deleteByBranchIdAndYearMonth(branchId, yearMonthStr);

            Salon finalSalon = salon;
            List<ReviewKeyword> toSave = freq.entrySet().stream()
                    .map(kv -> ReviewKeyword.builder()
                            .branch(branch)
                            .salon(finalSalon)
                            .keyword(kv.getKey())
                            .yearMonth(yearMonthStr)
                            .frequency(kv.getValue())
                            .build())
                    .toList();

            reviewKeywordRepository.saveAll(toSave);
        }

        log.info("[WordCloud] Đã tính xong từ khoá cho {} chi nhánh, tháng {}", reviewsByBranch.size(), yearMonthStr);
    }

    @Override
    @Transactional(readOnly = true)
    public WordCloudResponse getWordCloud(Long salonId, Long branchId, YearMonth yearMonth, int limit) {
        validateScope(salonId, branchId);

        YearMonth month = yearMonth != null ? yearMonth : YearMonth.now();
        String yearMonthStr = month.toString();
        int safeLimit = limit <= 0 ? 30 : Math.min(limit, 100);

        List<KeywordFrequencyResponse> keywords;
        if (branchId != null) {
            keywords = reviewKeywordRepository
                    .findByBranchIdAndYearMonthOrderByFrequencyDesc(branchId, yearMonthStr, PageRequest.of(0, safeLimit))
                    .stream()
                    .map(rk -> KeywordFrequencyResponse.builder()
                            .keyword(rk.getKeyword())
                            .frequency(rk.getFrequency().longValue())
                            .build())
                    .toList();
        } else {
            keywords = new ArrayList<>();
            List<Object[]> rows = reviewKeywordRepository
                    .findTopKeywordsBySalonIdAndYearMonth(salonId, yearMonthStr, PageRequest.of(0, safeLimit));
            for (Object[] row : rows) {
                keywords.add(KeywordFrequencyResponse.builder()
                        .keyword((String) row[0])
                        .frequency((Long) row[1])
                        .build());
            }
        }

        return WordCloudResponse.builder()
                .salonId(salonId)
                .branchId(branchId)
                .yearMonth(yearMonthStr)
                .keywords(keywords)
                .build();
    }

    // ---------------------------------------------------------------
    // Phân quyền — GIỐNG HỆT logic trong ReviewAnalyticsServiceImpl
    // (trùng lặp có chủ đích để 2 service độc lập nhau; nếu muốn tránh
    // trùng lặp, có thể tách ra 1 class dùng chung sau này)
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
            return;
        }

        if (branchId != null) {
            branchOwnershipValidator.validateOwnerBranch(branchId);
        } else {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            boolean isOwnSalon = salonRepository.findFirstByOwnerId(currentUserId)
                    .map(s -> s.getId().equals(salonId))
                    .orElse(false);
            if (!isOwnSalon) {
                throw new BusinessAccessDeniedException("Bạn không có quyền xem dữ liệu của salon này.");
            }
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
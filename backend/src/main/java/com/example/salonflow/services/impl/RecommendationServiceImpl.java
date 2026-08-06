package com.example.salonflow.services.impl;

import com.example.salonflow.dto.recommendation.RecommendationResponse;
import com.example.salonflow.dto.recommendation.ServiceRecommendationDto;
import com.example.salonflow.dto.recommendation.UserServiceUsageProjection;
import com.example.salonflow.entity.SalonService;
import com.example.salonflow.repository.BookingItemRepository;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.services.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final BookingItemRepository bookingItemRepository;
    private final ServiceRepository serviceRepository;

    // Cache trong bộ nhớ 1 giờ (TTL = 3600 giây)
    private static final long CACHE_TTL_MS = 60 * 60 * 1000L;
    private final Map<String, CacheEntry> recommendationCache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final RecommendationResponse response;
        final long expireAt;

        CacheEntry(RecommendationResponse response, long expireAt) {
            this.response = response;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(Long userId, Long branchId, Integer limit, String forceAbGroup) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 20) : 5;
        Long targetUserId = (userId != null && userId > 0) ? userId : 0L;

        // 1. Xác định nhóm A/B Test (TREATMENT = AI Collaborative Filtering, CONTROL = Popularity Fallback)
        String abGroup;
        if (forceAbGroup != null && (forceAbGroup.equalsIgnoreCase("TREATMENT") || forceAbGroup.equalsIgnoreCase("CONTROL"))) {
            abGroup = forceAbGroup.toUpperCase();
        } else {
            abGroup = (targetUserId > 0 && targetUserId % 2 == 0) ? "TREATMENT" : "CONTROL";
        }

        // 2. Kiểm tra Cache 1 giờ
        String cacheKey = targetUserId + "-" + (branchId != null ? branchId : 0) + "-" + maxLimit + "-" + abGroup;
        CacheEntry cached = recommendationCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("[Recommendation Cache Hit] Serving recommendations for key: {}", cacheKey);
            return cached.response;
        }

        List<ServiceRecommendationDto> resultDtos = new ArrayList<>();
        String algorithmUsed;

        // 3. Thực thi thuật toán dựa trên A/B Group
        if ("TREATMENT".equals(abGroup) && targetUserId > 0) {
            // Nhóm TREATMENT: Dùng Collaborative Filtering (Cosine Similarity)
            List<ServiceRecommendationDto> cfList = getCollaborativeFilteringRecommendations(targetUserId, branchId, maxLimit);
            if (!cfList.isEmpty()) {
                resultDtos.addAll(cfList);
                algorithmUsed = "COLLABORATIVE_FILTERING";

                // Nếu số lượng gợi ý AI chưa đủ limit, bù thêm dịch vụ phổ biến (Fallback)
                if (resultDtos.size() < maxLimit) {
                    List<ServiceRecommendationDto> fallbackList = getPopularityFallbackRecommendations(
                            branchId,
                            maxLimit - resultDtos.size(),
                            resultDtos.stream().map(ServiceRecommendationDto::getServiceId).collect(Collectors.toSet())
                    );
                    resultDtos.addAll(fallbackList);
                    algorithmUsed = "COLLABORATIVE_FILTERING_WITH_FALLBACK";
                }
            } else {
                // Cold start: Khách chưa có lịch sử $\rightarrow$ Chuyển sang Fallback
                resultDtos = getPopularityFallbackRecommendations(branchId, maxLimit, Collections.emptySet());
                algorithmUsed = "POPULARITY_FALLBACK";
            }
        } else {
            // Nhóm CONTROL hoặc khách vãng lai: Dùng Popularity Fallback
            resultDtos = getPopularityFallbackRecommendations(branchId, maxLimit, Collections.emptySet());
            algorithmUsed = "POPULARITY_FALLBACK";
        }

        RecommendationResponse response = RecommendationResponse.builder()
                .userId(targetUserId)
                .abGroup(abGroup)
                .algorithmUsed(algorithmUsed)
                .cachedAt(LocalDateTime.now())
                .recommendations(resultDtos)
                .build();

        // 4. Lưu Cache 1 giờ
        recommendationCache.put(cacheKey, new CacheEntry(response, System.currentTimeMillis() + CACHE_TTL_MS));
        log.info("[Recommendation Generated] userId={}, abGroup={}, algorithm={}, count={}",
                targetUserId, abGroup, algorithmUsed, resultDtos.size());

        return response;
    }

    /**
     * Thuật toán Collaborative Filtering dựa trên Cosine Similarity giữa các vector sử dụng dịch vụ của Users
     */
    private List<ServiceRecommendationDto> getCollaborativeFilteringRecommendations(Long targetUserId, Long branchId, int limit) {
        List<UserServiceUsageProjection> projections = bookingItemRepository.findUserServiceUsageVectors();
        if (projections.isEmpty()) {
            return Collections.emptyList();
        }

        // Dựng ma trận User-Service Vector: Map<UserId, Map<ServiceId, UsageCount>>
        Map<Long, Map<Long, Double>> userVectors = new HashMap<>();
        for (UserServiceUsageProjection p : projections) {
            if (p.getUserId() == null || p.getServiceId() == null) continue;
            userVectors.computeIfAbsent(p.getUserId(), k -> new HashMap<>())
                    .put(p.getServiceId(), (double) (p.getUsageCount() != null ? p.getUsageCount() : 1L));
        }

        Map<Long, Double> targetVector = userVectors.get(targetUserId);
        if (targetVector == null || targetVector.isEmpty()) {
            return Collections.emptyList();
        }

        // Tính Norm L2 cho Vector của Target User
        double normTarget = Math.sqrt(targetVector.values().stream().mapToDouble(v -> v * v).sum());
        if (normTarget == 0.0) {
            return Collections.emptyList();
        }

        // Tính Cosine Similarity giữa Target User và các Users khác
        Map<Long, Double> userSimilarities = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userVectors.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(targetUserId)) continue;

            Map<Long, Double> otherVector = entry.getValue();
            double dotProduct = 0.0;
            for (Map.Entry<Long, Double> sEntry : targetVector.entrySet()) {
                Long serviceId = sEntry.getKey();
                Double targetCount = sEntry.getValue();
                Double otherCount = otherVector.get(serviceId);
                if (otherCount != null) {
                    dotProduct += targetCount * otherCount;
                }
            }

            if (dotProduct > 0) {
                double normOther = Math.sqrt(otherVector.values().stream().mapToDouble(v -> v * v).sum());
                double similarity = dotProduct / (normTarget * normOther);
                if (similarity > 0) {
                    userSimilarities.put(otherUserId, similarity);
                }
            }
        }

        if (userSimilarities.isEmpty()) {
            return Collections.emptyList();
        }

        // Tính Điểm dự đoán (Predicted Score) cho các dịch vụ từ các User tương tự
        Map<Long, Double> candidateScores = new HashMap<>();
        for (Map.Entry<Long, Double> simEntry : userSimilarities.entrySet()) {
            Long otherUserId = simEntry.getKey();
            Double similarity = simEntry.getValue();
            Map<Long, Double> otherVector = userVectors.get(otherUserId);

            for (Map.Entry<Long, Double> sEntry : otherVector.entrySet()) {
                Long serviceId = sEntry.getKey();
                Double count = sEntry.getValue();
                // Ưu tiên gợi ý các dịch vụ tương tự
                candidateScores.merge(serviceId, similarity * count, Double::sum);
            }
        }

        if (candidateScores.isEmpty()) {
            return Collections.emptyList();
        }

        // Sắp xếp các dịch vụ theo Score giảm dần
        List<Map.Entry<Long, Double>> sortedCandidates = candidateScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        List<Long> candidateServiceIds = sortedCandidates.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Lấy thông tin entity chi tiết của dịch vụ
        List<SalonService> services = serviceRepository.findAllById(candidateServiceIds);
        Map<Long, SalonService> serviceMap = services.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> branchId == null || (s.getBranch() != null && s.getBranch().getId().equals(branchId)))
                .collect(Collectors.toMap(SalonService::getId, s -> s));

        List<ServiceRecommendationDto> dtoList = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : sortedCandidates) {
            Long serviceId = entry.getKey();
            Double score = entry.getValue();
            SalonService service = serviceMap.get(serviceId);
            if (service != null) {
                dtoList.add(mapToDto(service, score, "Gợi ý cá nhân hóa AI dựa trên hành vi khách hàng tương tự"));
                if (dtoList.size() >= limit) break;
            }
        }

        return dtoList;
    }

    /**
     * Fallback: Lấy các dịch vụ phổ biến nhất (Popularity Fallback)
     */
    private List<ServiceRecommendationDto> getPopularityFallbackRecommendations(Long branchId, int limit, Set<Long> excludeServiceIds) {
        Pageable pageable = PageRequest.of(0, limit + excludeServiceIds.size());
        List<Long> popularIds;

        if (branchId != null) {
            popularIds = bookingItemRepository.findTopPopularServiceIdsByBranch(branchId, pageable);
        } else {
            popularIds = bookingItemRepository.findTopPopularServiceIds(pageable);
        }

        // Loại bỏ các serviceId đã có trong kết quả trước đó
        List<Long> filteredIds = popularIds.stream()
                .filter(id -> !excludeServiceIds.contains(id))
                .collect(Collectors.toList());

        List<SalonService> services;
        if (!filteredIds.isEmpty()) {
            services = serviceRepository.findAllById(filteredIds);
        } else {
            // Nếu CSDL chưa có booking nào, lấy danh sách dịch vụ active mặc định
            if (branchId != null) {
                services = serviceRepository.findByBranchIdAndIsActiveTrue(branchId);
            } else {
                services = serviceRepository.findAll().stream()
                        .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                        .limit(limit)
                        .collect(Collectors.toList());
            }
        }

        List<ServiceRecommendationDto> dtoList = new ArrayList<>();
        double baseScore = 1.0;
        for (SalonService service : services) {
            if (excludeServiceIds.contains(service.getId())) continue;
            if (branchId != null && (service.getBranch() == null || !service.getBranch().getId().equals(branchId))) continue;

            dtoList.add(mapToDto(service, baseScore, "Dịch vụ phổ biến hàng đầu được yêu thích nhất"));
            baseScore -= 0.05;
            if (dtoList.size() >= limit) break;
        }

        return dtoList;
    }

    private ServiceRecommendationDto mapToDto(SalonService service, Double score, String reason) {
        String imageUrl = (service.getImages() != null && !service.getImages().isEmpty())
                ? service.getImages().get(0).getImageUrl()
                : null;

        return ServiceRecommendationDto.builder()
                .serviceId(service.getId())
                .name(service.getName())
                .price(service.getPrice())
                .durationMinutes(service.getDurationMinutes())
                .description(service.getDescription())
                .categoryName(service.getCategory() != null ? service.getCategory().getName() : null)
                .branchId(service.getBranch() != null ? service.getBranch().getId() : null)
                .branchName(service.getBranch() != null ? service.getBranch().getName() : null)
                .imageUrl(imageUrl)
                .score(score != null ? Math.round(score * 100.0) / 100.0 : 1.0)
                .reason(reason)
                .build();
    }
}

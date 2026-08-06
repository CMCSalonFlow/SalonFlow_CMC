package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.dto.hair.HairStyleAnalysisResult;
import com.example.salonflow.ai.dto.hair.HairStyleCandidateScoreRequest;
import com.example.salonflow.ai.dto.hair.HairStyleCandidateScoreResult;
import com.example.salonflow.ai.dto.hair.HairStyleImageResponse;
import com.example.salonflow.ai.dto.hair.HairStyleRecommendationItem;
import com.example.salonflow.ai.service.HairStyleRecommendationService;
import com.example.salonflow.entity.HairStyle;
import com.example.salonflow.entity.HairStyleImage;
import com.example.salonflow.entity.enums.hair.HairGender;
import com.example.salonflow.entity.enums.hair.HairMaintenanceLevel;
import com.example.salonflow.repository.HairStyleImageRepository;
import com.example.salonflow.repository.HairStyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HairStyleRecommendationServiceImpl implements HairStyleRecommendationService {

    private final HairStyleRepository hairStyleRepository;
    private final HairStyleImageRepository hairStyleImageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HairStyleRecommendationItem> recommend(HairStyleAnalysisResult analysis, HairGender gender, int limit) {
        if (limit <= 0) {
            limit = 5;
        }

        List<HairStyle> styles = resolveStyles(gender);
        if (styles.isEmpty()) {
            return List.of();
        }

        List<HairStyleRecommendationItem> scoredItems = new ArrayList<>();
        for (HairStyle style : styles) {
            HairStyleCandidateScoreRequest scoreRequest = toScoreRequest(analysis, style);
            HairStyleImage bestImage = selectBestImage(style);
            HairStyleCandidateScoreResult scoreResult = score(scoreRequest, bestImage);
            scoredItems.add(toRecommendationItem(style, bestImage, scoreResult));
        }

        List<HairStyleRecommendationItem> matched = scoredItems.stream()
                .filter(item -> item.finalScore() != null)
                .sorted(Comparator
                        .comparing(HairStyleRecommendationItem::finalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HairStyleRecommendationItem::ruleScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HairStyleRecommendationItem::aiScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HairStyleRecommendationItem::styleName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(limit)
                .toList();

        if (matched.size() >= limit) {
            return matched;
        }

        Set<Long> selectedIds = matched.stream()
                .map(HairStyleRecommendationItem::styleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<HairStyleRecommendationItem> fallback = scoredItems.stream()
                .filter(item -> !selectedIds.contains(item.styleId()))
                .sorted(Comparator
                        .comparing(HairStyleRecommendationItem::finalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HairStyleRecommendationItem::styleName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(limit - matched.size())
                .toList();

        List<HairStyleRecommendationItem> result = new ArrayList<>(matched);
        result.addAll(fallback);
        return result.stream().limit(limit).toList();
    }

    private List<HairStyle> resolveStyles(HairGender gender) {
        if (gender == null) {
            return hairStyleRepository.findByIsActiveTrueOrderByPopularityScoreDescSortOrderAscNameAsc();
        }
        return hairStyleRepository.findByIsActiveTrueAndGenderOrderByPopularityScoreDescSortOrderAscNameAsc(gender);
    }

    private HairStyleCandidateScoreRequest toScoreRequest(HairStyleAnalysisResult analysis, HairStyle style) {
        return new HairStyleCandidateScoreRequest(
                analysis,
                style.getId(),
                style.getCode(),
                style.getName(),
                style.getFaceShapeTags(),
                style.getHairTextureTags(),
                style.getHairLengthTags(),
                style.getHairDensityTags(),
                style.getDifficultyLevel(),
                style.getMaintenanceLevel(),
                style.getPopularityScore()
        );
    }

    private HairStyleCandidateScoreResult score(HairStyleCandidateScoreRequest request, HairStyleImage bestImage) {
        BigDecimal faceScore = scoreTokens(request.faceShapeTags(), enumToken(request.analysis().faceShape()));
        BigDecimal textureScore = scoreTokens(request.hairTextureTags(), enumToken(request.analysis().hairTexture()));
        BigDecimal lengthScore = scoreTokens(request.hairLengthTags(), enumToken(request.analysis().hairLength()));
        BigDecimal densityScore = scoreTokens(request.hairDensityTags(), enumToken(request.analysis().hairDensity()));

        BigDecimal ruleScore = faceScore.multiply(BigDecimal.valueOf(0.35))
                .add(textureScore.multiply(BigDecimal.valueOf(0.30)))
                .add(lengthScore.multiply(BigDecimal.valueOf(0.20)))
                .add(densityScore.multiply(BigDecimal.valueOf(0.15)));

        BigDecimal aiScore = imageScore(bestImage);
        BigDecimal popularity = normalize01(request.popularityScore());
        BigDecimal maintenance = maintenanceScore(request.maintenanceLevel());

        BigDecimal finalScore = ruleScore.multiply(BigDecimal.valueOf(0.60))
                .add(aiScore.multiply(BigDecimal.valueOf(0.15)))
                .add(popularity.multiply(BigDecimal.valueOf(0.15)))
                .add(maintenance.multiply(BigDecimal.valueOf(0.10)));

        return new HairStyleCandidateScoreResult(
                request.styleId(),
                request.styleCode(),
                scale(ruleScore),
                scale(popularity),
                scale(aiScore),
                scale(finalScore),
                buildReasons(request, faceScore, textureScore, lengthScore, densityScore, aiScore, popularity, maintenance)
        );
    }

    private HairStyleImage selectBestImage(HairStyle style) {
        List<HairStyleImage> images = hairStyleImageRepository
                .findByHairStyleIdAndIsActiveTrueOrderByIsCoverDescDisplayOrderAscIdAsc(style.getId());

        if (images == null || images.isEmpty()) {
            return null;
        }

        return images.stream()
                .max(Comparator
                        .comparing(HairStyleImage::getIsCover, Comparator.nullsLast(Boolean::compareTo))
                        .thenComparing(HairStyleImage::getImageQualityScore, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(HairStyleImage::getAiAestheticScore, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(HairStyleImage::getDisplayOrder, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HairStyleImage::getId))
                .orElse(images.get(0));
    }

    private HairStyleRecommendationItem toRecommendationItem(HairStyle style, HairStyleImage image, HairStyleCandidateScoreResult scoreResult) {
        HairStyleImageResponse imageResponse = null;
        if (image != null) {
            imageResponse = new HairStyleImageResponse(
                    image.getId(),
                    image.getMedia() != null ? image.getMedia().getUrl() : null,
                    image.getIsCover(),
                    image.getDisplayOrder(),
                    image.getImageQualityScore(),
                    image.getAiAestheticScore()
            );
        }

        String priceRange = null;
        if (style.getPriceMin() != null || style.getPriceMax() != null) {
            priceRange = (style.getPriceMin() != null ? style.getPriceMin().toPlainString() : "?")
                    + " - "
                    + (style.getPriceMax() != null ? style.getPriceMax().toPlainString() : "?");
        }

        return new HairStyleRecommendationItem(
                style.getId(),
                style.getCode(),
                style.getName(),
                style.getDescription(),
                imageResponse,
                style.getDifficultyLevel(),
                style.getMaintenanceLevel(),
                priceRange,
                scoreResult != null ? scoreResult.ruleScore() : null,
                scoreResult != null ? scoreResult.aiScore() : null,
                scoreResult != null ? scoreResult.finalScore() : null,
                scoreResult != null ? scoreResult.reasons() : List.of()
        );
    }

    private BigDecimal scoreTokens(String tags, String targetToken) {
        if (targetToken == null || targetToken.isBlank()) {
            return BigDecimal.valueOf(0.5);
        }
        Set<String> tokens = parseTokens(tags);
        if (tokens.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (tokens.contains("all") || tokens.contains("any") || tokens.contains("universal")) {
            return BigDecimal.valueOf(0.75);
        }
        if (tokens.contains(targetToken)) {
            return BigDecimal.ONE;
        }
        for (String token : tokens) {
            if (token.contains(targetToken) || targetToken.contains(token)) {
                return BigDecimal.valueOf(0.85);
            }
        }
        return BigDecimal.ZERO;
    }

    private Set<String> parseTokens(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }
        String normalized = tags.toLowerCase(Locale.ROOT)
                .replace('|', ',')
                .replace(';', ',')
                .replace('/', ',');
        String[] parts = normalized.split("[,\\s]+");
        Set<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            String token = normalizeToken(part);
            if (!token.isBlank()) {
                result.add(token);
            }
        }
        return result;
    }

    private String enumToken(Enum<?> value) {
        if (value == null) {
            return "";
        }
        return normalizeToken(value.name());
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        return token.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private BigDecimal maintenanceScore(HairMaintenanceLevel level) {
        if (level == null) {
            return BigDecimal.valueOf(0.5);
        }
        return switch (level) {
            case LOW -> BigDecimal.ONE;
            case MEDIUM -> BigDecimal.valueOf(0.7);
            case HIGH -> BigDecimal.valueOf(0.4);
        };
    }

    private BigDecimal imageScore(HairStyleImage image) {
        if (image == null) {
            return BigDecimal.valueOf(0.5);
        }
        BigDecimal quality = normalize01(image.getImageQualityScore());
        BigDecimal aesthetic = normalize01(image.getAiAestheticScore());
        return quality.multiply(BigDecimal.valueOf(0.6))
                .add(aesthetic.multiply(BigDecimal.valueOf(0.4)));
    }

    private BigDecimal normalize01(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.5);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal normalized = value;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            normalized = BigDecimal.ZERO;
        } else if (normalized.compareTo(BigDecimal.ONE) > 0) {
            normalized = BigDecimal.ONE;
        }
        return normalized.setScale(4, RoundingMode.HALF_UP);
    }

    private List<String> buildReasons(
            HairStyleCandidateScoreRequest request,
            BigDecimal faceScore,
            BigDecimal textureScore,
            BigDecimal lengthScore,
            BigDecimal densityScore,
            BigDecimal aiScore,
            BigDecimal popularity,
            BigDecimal maintenance
    ) {
        List<String> reasons = new ArrayList<>();
        addReason(reasons, faceScore, "Phù hợp khuôn mặt " + safeEnumLabel(request.analysis().faceShape()));
        addReason(reasons, textureScore, "Phù hợp texture " + safeEnumLabel(request.analysis().hairTexture()));
        addReason(reasons, lengthScore, "Phù hợp độ dài tóc " + safeEnumLabel(request.analysis().hairLength()));
        addReason(reasons, densityScore, "Phù hợp độ dày tóc " + safeEnumLabel(request.analysis().hairDensity()));
        addReason(reasons, maintenance, "Mức chăm sóc phù hợp");
        addReason(reasons, popularity, "Đang được ưa chuộng");
        addReason(reasons, aiScore, "Ảnh mẫu hiển thị rõ kiểu tóc");
        if (reasons.isEmpty()) {
            reasons.add("Gợi ý dựa trên độ phổ biến và dữ liệu kiểu tóc trong DB");
        }
        return reasons;
    }

    private void addReason(List<String> reasons, BigDecimal score, String reason) {
        if (score != null && score.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            reasons.add(reason);
        }
    }

    private String safeEnumLabel(Enum<?> value) {
        if (value == null) {
            return "unknown";
        }
        return value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}

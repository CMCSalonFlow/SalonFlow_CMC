package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.dto.hair.*;
import com.example.salonflow.ai.provider.HairVisionProvider;
import com.example.salonflow.ai.service.HairStyleAnalysisService;
import com.example.salonflow.ai.service.HairStyleRecommendationService;
import com.example.salonflow.config.properties.MinioProperties;
import com.example.salonflow.entity.CustomerHairProfile;
import com.example.salonflow.entity.HairAnalysisResult;
import com.example.salonflow.entity.HairStyle;
import com.example.salonflow.entity.HairStyleImage;
import com.example.salonflow.entity.MediaFile;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.enums.hair.HairAnalysisStatus;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.CustomerHairProfileRepository;
import com.example.salonflow.repository.HairAnalysisResultRepository;
import com.example.salonflow.repository.HairStyleImageRepository;
import com.example.salonflow.repository.HairStyleRepository;
import com.example.salonflow.repository.MediaFileRepository;
import com.example.salonflow.repository.UserRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HairStyleAnalysisServiceImpl implements HairStyleAnalysisService {

    private static final String ANALYSIS_VERSION = "v1";

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final HairAnalysisResultRepository hairAnalysisResultRepository;
    private final CustomerHairProfileRepository customerHairProfileRepository;
    private final HairStyleRepository hairStyleRepository;
    private final HairStyleImageRepository hairStyleImageRepository;
    private final HairVisionProvider hairVisionProvider;
    private final HairStyleRecommendationService hairStyleRecommendationService;
    private final AiProperties aiProperties;

    @Override
    @Transactional
    public HairStyleRecommendationResponse analyze(Long userId, HairStyleAnalyzeRequest request) {
        validateAnalyzeRequest(userId, request);

        if (!aiProperties.isEnabled() || aiProperties.getHair() == null || !aiProperties.getHair().isEnabled()) {
            throw new BadRequestException("Hair AI analysis is disabled");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MediaFile mediaFile = mediaFileRepository.findById(request.mediaId())
                .orElseThrow(() -> new ResourceNotFoundException("Media not found"));

        HairAnalysisResult analysisResult = HairAnalysisResult.builder()
                .user(user)
                .media(mediaFile)
                .status(HairAnalysisStatus.PROCESSING)
                .analysisVersion(ANALYSIS_VERSION)
                .provider(aiProperties.getHair().getProvider())
                .build();
        analysisResult = hairAnalysisResultRepository.save(analysisResult);

        HairVisionAnalysisRequest visionRequest = buildVisionRequest(mediaFile);
        HairVisionAnalysisResult visionResult = hairVisionProvider.analyze(visionRequest);

        analysisResult.setStatus(HairAnalysisStatus.COMPLETED);
        analysisResult.setFaceShape(visionResult.faceShape());
        analysisResult.setHairTexture(visionResult.hairTexture());
        analysisResult.setHairLength(visionResult.hairLength());
        analysisResult.setHairDensity(visionResult.hairDensity());
        analysisResult.setCurrentStyle(visionResult.currentStyle());
        analysisResult.setConfidence(normalizeConfidence(visionResult.confidence()));
        analysisResult.setRawResponse(visionResult.rawResponse());
        analysisResult.setAnalyzedAt(Instant.now());
        analysisResult.setProvider(visionResult.provider());
        analysisResult = hairAnalysisResultRepository.save(analysisResult);

        upsertCustomerProfile(user, analysisResult);

        HairStyleAnalysisResult analysisDto = toAnalysisDto(analysisResult);
        List<HairStyleRecommendationItem> suggestions = hairStyleRecommendationService.recommend(analysisDto, 5);
        return new HairStyleRecommendationResponse(
                analysisResult.getId(),
                analysisDto,
                suggestions,
                analysisResult.getProvider(),
                analysisResult.getAnalyzedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HairStyleProfileResponse getProfile(Long userId) {
        CustomerHairProfile profile = customerHairProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Hair profile not found"));

        HairStyleRecommendationItem selectedStyle = null;
        if (profile.getSelectedHairStyle() != null) {
            selectedStyle = toRecommendationItem(profile.getSelectedHairStyle(), profile.getSelectedHairStyleImage(), null, null, List.of());
        }

        HairStyleAnalysisResult analysis = null;
        if (profile.getLatestAnalysisResult() != null) {
            analysis = toAnalysisDto(profile.getLatestAnalysisResult());
        }

        return new HairStyleProfileResponse(
                userId,
                selectedStyle,
                analysis,
                profile.getFaceShape(),
                profile.getHairTexture(),
                profile.getHairLength(),
                profile.getHairDensity(),
                profile.getCurrentStyle(),
                profile.getProfileSyncedAt()
        );
    }

    @Override
    @Transactional
    public HairStyleProfileResponse confirmSelection(Long userId, HairStyleConfirmRequest request) {
        if (request == null || request.styleId() == null) {
            throw new BadRequestException("Style id is required");
        }

        CustomerHairProfile profile = customerHairProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Hair profile not found"));

        HairStyle style = hairStyleRepository.findById(request.styleId())
                .orElseThrow(() -> new ResourceNotFoundException("Hair style not found"));

        HairStyleImage image = null;
        if (request.styleImageId() != null) {
            image = hairStyleImageRepository.findById(request.styleImageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hair style image not found"));
            if (!Objects.equals(image.getHairStyle().getId(), style.getId())) {
                throw new BadRequestException("Style image does not belong to selected style");
            }
        }

        profile.setSelectedHairStyle(style);
        profile.setSelectedHairStyleImage(image);
        profile.setProfileSyncedAt(Instant.now());
        profile = customerHairProfileRepository.save(profile);

        HairStyleRecommendationItem selectedStyle = toRecommendationItem(style, image, null, null, List.of("selected_by_user"));
        HairStyleAnalysisResult analysis = profile.getLatestAnalysisResult() != null
                ? toAnalysisDto(profile.getLatestAnalysisResult())
                : null;

        return new HairStyleProfileResponse(
                userId,
                selectedStyle,
                analysis,
                profile.getFaceShape(),
                profile.getHairTexture(),
                profile.getHairLength(),
                profile.getHairDensity(),
                profile.getCurrentStyle(),
                profile.getProfileSyncedAt()
        );
    }

    private void validateAnalyzeRequest(Long userId, HairStyleAnalyzeRequest request) {
        if (userId == null) {
            throw new BadRequestException("User id is required");
        }
        if (request == null || request.mediaId() == null) {
            throw new BadRequestException("Media id is required");
        }
    }

    private HairVisionAnalysisRequest buildVisionRequest(MediaFile mediaFile) {
        StandardizedImageInput standardized = loadAndStandardizeImage(mediaFile);
        return new HairVisionAnalysisRequest(
                mediaFile.getId(),
                standardized.dataUrl(),
                standardized.mimeType(),
                mediaFile.getOriginalFileName(),
                standardized.fileSize()
        );
    }

    private StandardizedImageInput loadAndStandardizeImage(MediaFile mediaFile) {
        String objectName = resolveObjectName(mediaFile);
        String bucket = resolveBucket(mediaFile);

        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        )) {
            byte[] originalBytes = inputStream.readAllBytes();
            String originalContentType = resolveContentType(mediaFile);

            BufferedImage decoded = tryDecodeImage(originalBytes);
            if (decoded == null) {
                return new StandardizedImageInput(
                        toDataUrl(originalBytes, originalContentType),
                        originalContentType,
                        (long) originalBytes.length
                );
            }

            BufferedImage resized = Thumbnails.of(decoded)
                    .size(1024, 1024)
                    .keepAspectRatio(true)
                    .outputFormat("png")
                    .asBufferedImage();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", output);
            byte[] normalizedBytes = output.toByteArray();

            return new StandardizedImageInput(
                    toDataUrl(normalizedBytes, "image/png"),
                    "image/png",
                    (long) normalizedBytes.length
            );
        } catch (Exception ex) {
            throw new BadRequestException("Failed to load or normalize image: " + ex.getMessage());
        }
    }

    private BufferedImage tryDecodeImage(byte[] bytes) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(input);
        } catch (Exception ex) {
            return null;
        }
    }

    private String toDataUrl(byte[] bytes, String mimeType) {
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String resolveBucket(MediaFile mediaFile) {
        if (mediaFile.getBucket() != null && !mediaFile.getBucket().isBlank()) {
            return mediaFile.getBucket();
        }
        return minioProperties.getBucketName();
    }

    private String resolveObjectName(MediaFile mediaFile) {
        if (mediaFile.getObjectName() != null && !mediaFile.getObjectName().isBlank()) {
            return mediaFile.getObjectName();
        }
        if (mediaFile.getUrl() != null && mediaFile.getBucket() != null && !mediaFile.getBucket().isBlank()) {
            String marker = "/" + mediaFile.getBucket() + "/";
            int index = mediaFile.getUrl().indexOf(marker);
            if (index >= 0) {
                return mediaFile.getUrl().substring(index + marker.length());
            }
        }
        throw new BadRequestException("Media file does not contain a valid object name");
    }

    private String resolveContentType(MediaFile mediaFile) {
        if (mediaFile.getContentType() != null && !mediaFile.getContentType().isBlank()) {
            return mediaFile.getContentType();
        }
        return "image/png";
    }

    private void upsertCustomerProfile(User user, HairAnalysisResult analysisResult) {
        CustomerHairProfile profile = customerHairProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> CustomerHairProfile.builder()
                        .user(user)
                        .build());

        profile.setFaceShape(analysisResult.getFaceShape());
        profile.setHairTexture(analysisResult.getHairTexture());
        profile.setHairLength(analysisResult.getHairLength());
        profile.setHairDensity(analysisResult.getHairDensity());
        profile.setCurrentStyle(analysisResult.getCurrentStyle());
        profile.setLatestAnalysisResult(analysisResult);
        profile.setProfileSyncedAt(Instant.now());
        customerHairProfileRepository.save(profile);
    }

    private HairStyleAnalysisResult toAnalysisDto(HairAnalysisResult entity) {
        return new HairStyleAnalysisResult(
                entity.getFaceShape(),
                entity.getHairTexture(),
                entity.getHairLength(),
                entity.getHairDensity(),
                entity.getCurrentStyle(),
                entity.getConfidence(),
                entity.getProvider(),
                entity.getRawResponse()
        );
    }

    private HairStyleRecommendationItem toRecommendationItem(
            HairStyle style,
            HairStyleImage image,
            BigDecimal ruleScore,
            BigDecimal aiScore,
            List<String> reasons
    ) {
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
                ruleScore,
                aiScore,
                scoreFinal(ruleScore, aiScore, style.getPopularityScore()),
                reasons
        );
    }

    private BigDecimal scoreFinal(BigDecimal ruleScore, BigDecimal aiScore, BigDecimal popularityScore) {
        BigDecimal rule = ruleScore != null ? ruleScore : BigDecimal.ZERO;
        BigDecimal ai = aiScore != null ? aiScore : BigDecimal.ZERO;
        BigDecimal popularity = popularityScore != null ? popularityScore : BigDecimal.ZERO;
        return rule.multiply(BigDecimal.valueOf(0.65))
                .add(ai.multiply(BigDecimal.valueOf(0.25)))
                .add(popularity.multiply(BigDecimal.valueOf(0.10)));
    }

    private BigDecimal normalizeConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return BigDecimal.valueOf(0.50);
        }
        if (confidence.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return confidence;
    }

    private record StandardizedImageInput(
            String dataUrl,
            String mimeType,
            Long fileSize
    ) {
    }
}

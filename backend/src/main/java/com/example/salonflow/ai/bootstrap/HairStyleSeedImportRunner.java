package com.example.salonflow.ai.bootstrap;

import com.example.salonflow.config.properties.MinioProperties;
import com.example.salonflow.entity.HairStyle;
import com.example.salonflow.entity.HairStyleImage;
import com.example.salonflow.entity.MediaFile;
import com.example.salonflow.entity.enums.hair.HairDifficultyLevel;
import com.example.salonflow.entity.enums.hair.HairGender;
import com.example.salonflow.entity.enums.hair.HairMaintenanceLevel;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.repository.HairStyleImageRepository;
import com.example.salonflow.repository.HairStyleRepository;
import com.example.salonflow.repository.MediaFileRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.hair-style-import", name = "enabled", havingValue = "true")
public class HairStyleSeedImportRunner implements CommandLineRunner {

    private final HairStyleSeedImportProperties properties;
    private final HairStyleRepository hairStyleRepository;
    private final HairStyleImageRepository hairStyleImageRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void run(String... args) throws Exception {
        importArchive(HairGender.MEN, properties.getManZipPath());
        importArchive(HairGender.WOMEN, properties.getWomenZipPath());
        log.info("Hair style seed import completed successfully");
    }

    private void importArchive(HairGender archiveGender, String zipPath) throws Exception {
        if (!StringUtils.hasText(zipPath)) {
            throw new BadRequestException("Hair style import zip path is missing for " + archiveGender);
        }

        Resource resource = zipPath.startsWith("classpath:")
                ? new ClassPathResource(zipPath.substring("classpath:".length()))
                : new FileSystemResource(zipPath);

        if (!resource.exists()) {
            throw new BadRequestException("Hair style zip file not found: " + zipPath);
        }

        File file;
        File tempFile = null;
        try {
            file = resource.getFile();
        } catch (IOException e) {
            Path tempPath = Files.createTempFile("hairstyle_" + archiveGender.name().toLowerCase(Locale.ROOT), ".zip");
            tempFile = tempPath.toFile();
            tempFile.deleteOnExit();
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }
            file = tempFile;
        }

        List<HairStyleSeedItem> seeds = HairStyleSeedCatalog.all().stream()
                .filter(item -> item.gender() == archiveGender)
                .toList();

        if (seeds.isEmpty()) {
            log.warn("No hair style seed items found for gender {}", archiveGender);
            return;
        }

        try (ZipFile zipFile = new ZipFile(file)) {
            Map<String, List<ZipAsset>> assetsByPrefix = loadAssets(zipFile);
            for (HairStyleSeedItem seed : seeds) {
                importStyle(seed, assetsByPrefix);
            }
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    tempFile.delete();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void importStyle(
            HairStyleSeedItem seed,
            Map<String, List<ZipAsset>> assetsByPrefix
    ) throws Exception {
        String normalizedPrefix = normalizeKey(seed.imagePrefix());
        List<ZipAsset> matches = assetsByPrefix.entrySet().stream()
                .filter(entry -> entry.getKey().equals(normalizedPrefix)
                        || entry.getKey().startsWith(normalizedPrefix + " "))
                .flatMap(entry -> entry.getValue().stream())
                .sorted(Comparator.comparing(ZipAsset::fileName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (matches.isEmpty()) {
            log.warn("No images found for hair style {} ({})", seed.name(), seed.imagePrefix());
        }

        HairStyle style = hairStyleRepository.findByCode(seed.code()).orElseGet(HairStyle::new);
        style.setCode(seed.code());
        style.setName(seed.name());
        style.setGender(seed.gender());
        style.setDescription(buildDescription(seed.name(), seed.gender()));
        style.setFaceShapeTags(deriveFaceShapeTags(seed.name()));
        style.setHairTextureTags(deriveTextureTags(seed.name()));
        style.setHairLengthTags(deriveLengthTags(seed.name()));
        style.setHairDensityTags(deriveDensityTags(seed.name()));
        style.setDifficultyLevel(deriveDifficulty(seed.name()));
        style.setMaintenanceLevel(deriveMaintenance(seed.name()));
        style.setPriceMin(derivePriceMin(seed.name()));
        style.setPriceMax(derivePriceMax(seed.name()));
        style.setPopularityScore(BigDecimal.valueOf(seed.popularityScore()));
        style.setIsActive(true);
        style.setSortOrder(seed.sortOrder());
        style = hairStyleRepository.save(style);

        for (int index = 0; index < matches.size(); index++) {
            ZipAsset asset = matches.get(index);
            String objectName = buildObjectName(seed.gender(), seed.code(), asset.fileName());
            MediaFile media = ensureAssetUploaded(objectName, asset);

            Optional<HairStyleImage> existingImage = hairStyleImageRepository
                    .findByHairStyleIdAndMediaId(style.getId(), media.getId());
            if (existingImage.isPresent()) {
                continue;
            }

            HairStyleImage image = HairStyleImage.builder()
                    .hairStyle(style)
                    .media(media)
                    .isCover(index == 0)
                    .displayOrder(index)
                    .imageQualityScore(BigDecimal.valueOf(0.90))
                    .aiAestheticScore(BigDecimal.valueOf(0.85))
                    .isActive(true)
                    .build();
            hairStyleImageRepository.save(image);
        }

        log.info("Imported hair style {} with {} image(s)", seed.code(), matches.size());
    }

    private MediaFile ensureAssetUploaded(String objectName, ZipAsset asset) {
        if (!doesObjectExistInMinio(objectName)) {
            uploadToMinio(objectName, asset);
        }

        return mediaFileRepository.findByObjectName(objectName)
                .orElseGet(() -> {
                    MediaFile media = MediaFile.builder()
                            .objectName(objectName)
                            .originalFileName(asset.fileName())
                            .contentType(asset.contentType())
                            .fileSize((long) asset.bytes().length)
                            .provider("MINIO")
                            .bucket(minioProperties.getBucketName())
                            .url(buildPublicUrl(objectName))
                            .build();
                    return mediaFileRepository.save(media);
                });
    }

    private boolean doesObjectExistInMinio(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void uploadToMinio(String objectName, ZipAsset asset) {
        try (InputStream inputStream = new ByteArrayInputStream(asset.bytes())) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, asset.bytes().length, -1)
                            .contentType(asset.contentType())
                            .build()
            );
            log.info("Uploaded hair style asset to MinIO: {}", objectName);
        } catch (Exception ex) {
            throw new BadRequestException("Failed to upload hair style asset " + asset.fileName() + ": " + ex.getMessage());
        }
    }

    private Map<String, List<ZipAsset>> loadAssets(ZipFile zipFile) throws Exception {
        Map<String, List<ZipAsset>> assets = new LinkedHashMap<>();
        var entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }

            String fileName = Path.of(entry.getName()).getFileName().toString();
            String normalizedStem = normalizeStem(fileName);
            if (!isSupportedImage(fileName)) {
                continue;
            }

            byte[] bytes;
            try (InputStream input = zipFile.getInputStream(entry)) {
                bytes = input.readAllBytes();
            }

            assets.computeIfAbsent(normalizedStem, key -> new ArrayList<>())
                    .add(new ZipAsset(fileName, bytes, detectContentType(fileName)));
        }

        return assets;
    }

    private String buildObjectName(HairGender gender, String code, String fileName) {
        return properties.getObjectPrefix()
                + "/"
                + gender.name().toLowerCase(Locale.ROOT)
                + "/"
                + code.toLowerCase(Locale.ROOT)
                + "/"
                + sanitizeObjectSegment(fileName);
    }

    private String buildPublicUrl(String objectName) {
        String endpoint = minioProperties.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + minioProperties.getBucketName() + "/" + objectName;
    }

    private boolean isSupportedImage(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".avif");
    }

    private String detectContentType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".avif")) {
            return "image/avif";
        }
        return "image/jpeg";
    }

    private String sanitizeObjectSegment(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
        if (!normalized.contains(".")) {
            normalized = normalized + ".png";
        }
        return normalized;
    }

    private String normalizeStem(String fileName) {
        String name = fileName;
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.replaceAll("\\s*\\(\\d+\\)$", "");
        name = name.replaceAll("[-_ ]\\d+$", "");
        return normalizeKey(name);
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String buildDescription(String name, HairGender gender) {
        if (gender == HairGender.MEN) {
            return "Kiểu tóc " + name + " mang lại diện mạo gọn gàng, nam tính và hiện đại, phù hợp nhiều phong cách cá nhân khác nhau.";
        }
        return "Kiểu tóc " + name + " mang lại vẻ đẹp mềm mại, thời trang và nổi bật, giúp tôn nét thanh lịch của khách hàng.";
    }

    private String deriveFaceShapeTags(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "buzz", "crew", "crop", "pixie", "bob", "lob")) {
            return "oval,round,heart,square";
        }
        if (containsAny(lower, "long", "waves", "curl", "perm", "bun", "knot")) {
            return "oval,round,diamond,oblong";
        }
        if (containsAny(lower, "mohawk", "mullet", "hawk", "undercut", "quiff", "pompadour")) {
            return "oval,round,square,heart";
        }
        return "oval,round,square";
    }

    private String deriveTextureTags(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "curl", "perm", "waves", "barrel", "hime")) {
            return "wavy,curly";
        }
        if (containsAny(lower, "straight", "slicked", "pompadour", "quiff", "crop", "crew", "buzz", "side part", "bob")) {
            return "straight,wavy";
        }
        return "straight,wavy,curly";
    }

    private String deriveLengthTags(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "buzz", "crew", "crop", "pixie", "short", "faux hawk", "side part", "pompadour", "slicked back undercut")) {
            return "short,medium";
        }
        if (containsAny(lower, "bob", "lob", "shoulder", "quiff", "layer two block", "hime")) {
            return "medium,long";
        }
        if (containsAny(lower, "long", "bun", "knot", "waves", "curl", "perm", "mullet", "wolf")) {
            return "long,very_long";
        }
        return "medium,long";
    }

    private String deriveDensityTags(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "buzz", "crew", "pixie", "crop", "side part")) {
            return "low,medium";
        }
        if (containsAny(lower, "curl", "perm", "waves", "mullet", "layer", "long", "bob", "lob")) {
            return "medium,high";
        }
        return "medium";
    }

    private HairDifficultyLevel deriveDifficulty(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "buzz", "crew", "crop", "pixie", "side part", "bob", "lob", "short layered hair", "short quiff")) {
            return HairDifficultyLevel.EASY;
        }
        if (containsAny(lower, "long curly hair", "long layered hair", "long quiff", "man bun", "mohawk", "pompadour", "high layered cut", "hime cut", "shoulder-length flipped-out hair", "toc bob", "toc tem pixie", "victoria beckham bob")) {
            return HairDifficultyLevel.MEDIUM;
        }
        return HairDifficultyLevel.HARD;
    }

    private HairMaintenanceLevel deriveMaintenance(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "buzz", "crew", "crop", "pixie", "side part", "bob", "lob", "short quiff")) {
            return HairMaintenanceLevel.LOW;
        }
        if (containsAny(lower, "long curly hair", "long layered hair", "man bun", "top knot", "hippie perm", "korean volume perm", "layer wolf cut", "layered tomboy mullet", "shoulder-length layered mullet", "large barrel curls", "long loose waves", "french end curls")) {
            return HairMaintenanceLevel.HIGH;
        }
        return HairMaintenanceLevel.MEDIUM;
    }

    private BigDecimal derivePriceMin(String name) {
        HairDifficultyLevel difficulty = deriveDifficulty(name);
        return switch (difficulty) {
            case EASY -> BigDecimal.valueOf(120000);
            case MEDIUM -> BigDecimal.valueOf(180000);
            case HARD -> BigDecimal.valueOf(250000);
        };
    }

    private BigDecimal derivePriceMax(String name) {
        HairDifficultyLevel difficulty = deriveDifficulty(name);
        return switch (difficulty) {
            case EASY -> BigDecimal.valueOf(250000);
            case MEDIUM -> BigDecimal.valueOf(450000);
            case HARD -> BigDecimal.valueOf(700000);
        };
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private record ZipAsset(
            String fileName,
            byte[] bytes,
            String contentType
    ) {
    }
}

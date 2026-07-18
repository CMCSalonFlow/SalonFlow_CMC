package com.example.salonflow.services.impl;


import com.example.salonflow.config.properties.MinioProperties;
import com.example.salonflow.dto.Upload.UploadResponse;
import com.example.salonflow.entity.MediaFile;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.MediaFileRepository;
import com.example.salonflow.services.service.MediaService;
import com.example.salonflow.util.ImageValidator;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MinioClient minioClient;
    private final MediaFileRepository repository;
    private final MinioProperties properties;


    
    @Override
    @Transactional
    public UploadResponse upload(MultipartFile file) {

        ImageValidator.validate(file);

        try {

            // 1. Resize
            BufferedImage resized =
                    Thumbnails.of(file.getInputStream())
                            .size(800, 600)
                            .keepAspectRatio(true)
                            .asBufferedImage();

            // 2. Convert
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            ImageIO.write(resized, "png", os);

            byte[] bytes = os.toByteArray();

            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(bytes);

            // 3. Object name
            String objectName =
                    "media/" +
                    LocalDate.now().getYear() + "/" +
                    LocalDate.now().getMonthValue() + "/" +
                    UUID.randomUUID() +
                    ".png";

            // 4. Upload MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, bytes.length, -1)
                            .contentType("image/png")
                            .build()
            );

            // 5. URL
            String url =
                    properties.getEndpoint() + "/" +
                    properties.getBucketName() + "/" +
                    objectName;

            // 6. Save DB
            MediaFile media = MediaFile.builder()
                    .objectName(objectName)
                    .url(url)
                    .contentType("image/png")
                    .fileSize((long) bytes.length)
                    .provider("MINIO")
                    .bucket(properties.getBucketName())
                    .originalFileName(file.getOriginalFilename())
                    .build();

                    

            media = repository.save(media);


            // 7. Response
            return UploadResponse.builder()
                    .id(media.getId())
                    .url(media.getUrl())
                    .fileName(media.getObjectName())
                    .build();

        } catch (Exception e) {
                e.printStackTrace();
                throw new BadRequestException("Upload failed: " + e.getMessage());
                }
    }

    @Override
    public void delete(Long id) {

        MediaFile media = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Media not found"));

        try {

            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(media.getBucket())
                            .object(media.getObjectName())
                            .build()
            );

            repository.delete(media);

        } catch (Exception e) {
            throw new BadRequestException("Delete failed");
        }
    }

    @Override
    @Transactional
    public String uploadInvoice(File pdfFile, Long bookingId) {

    try (FileInputStream input = new FileInputStream(pdfFile)) {

       String objectName =
        "invoice/"
        + LocalDate.now().getYear()
        + "/"
        + LocalDate.now().getMonthValue()
        + "/"
        + bookingId
        + "-"
        + UUID.randomUUID()
        + ".pdf";

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(objectName)
                        .stream(input, pdfFile.length(), -1)
                        .contentType("application/pdf")
                        .build()
        );

        String url = generatePresignedUrl(objectName);

       MediaFile media = MediaFile.builder()
        .objectName(objectName)
        .originalFileName("invoice-" + bookingId + ".pdf")
        .contentType("application/pdf")
        .fileSize(pdfFile.length())
        .provider("MINIO")
        .bucket(properties.getBucketName())
        .url(objectName) // hoặc để null nếu không dùng cột này
        .build();

        repository.save(media);

        return objectName;

    } catch (Exception e) {

        throw new BadRequestException(
                "Upload invoice failed: " + e.getMessage()
        );

    }

 }
 private String generatePresignedUrl(String objectName) {
    try {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(7, TimeUnit.DAYS)
                        .build()
        );
    } catch (Exception e) {
        throw new BadRequestException("Cannot create presigned url: " + e.getMessage());
    }
 }

    @Override
    public String getInvoiceUrl(String objectName) {
    return generatePresignedUrl(objectName);
}
}
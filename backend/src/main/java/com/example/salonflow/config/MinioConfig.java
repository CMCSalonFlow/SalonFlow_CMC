package com.example.salonflow.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.salonflow.config.properties.MinioProperties;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@Slf4j
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(
                        properties.getAccessKey(),
                        properties.getSecretKey()
                )
                .build();

        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucketName()).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(properties.getBucketName()).build()
                );
                log.info("Created MinIO bucket: {}", properties.getBucketName());
            }

            // Cấu hình Policy PUBLIC READ cho phép trình duyệt (HTML img) xem được ảnh không bị 403 AccessDenied
            String policyJson = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(properties.getBucketName());

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(properties.getBucketName())
                            .config(policyJson)
                            .build()
            );
            log.info("Set MinIO public read policy for bucket: {}", properties.getBucketName());

        } catch (Exception e) {
            log.warn("MinIO bucket policy initialization warning: {}", e.getMessage());
        }

        return minioClient;
    }
}

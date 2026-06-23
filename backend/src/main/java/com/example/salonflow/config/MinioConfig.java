package com.example.salonflow.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.salonflow.config.properties.MinioProperties;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            MinioProperties properties) {

        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(
                        properties.getAccessKey(),
                        properties.getSecretKey()
                )
                .build();
    }
}

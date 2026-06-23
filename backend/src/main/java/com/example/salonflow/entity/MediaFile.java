package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // tên file lưu trong MinIO
    private String objectName;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    private String url;

    private String provider; // MINIO / S3 / CLOUDINARY

    private String bucket;
}
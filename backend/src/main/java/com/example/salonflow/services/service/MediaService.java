package com.example.salonflow.services.service;

import com.example.salonflow.dto.Upload.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface MediaService {

    UploadResponse upload(MultipartFile file);

    void delete(Long mediaId);

    String uploadInvoice(File pdfFile, Long bookingId);

    // Sinh Presigned URL từ objectName
    String getInvoiceUrl(String objectName);
}
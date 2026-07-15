package com.example.salonflow.services.service;

import org.springframework.web.multipart.MultipartFile;

import com.example.salonflow.dto.Upload.UploadResponse;

public interface MediaService {

    UploadResponse upload( MultipartFile file);

    void delete(Long mediaId);
}
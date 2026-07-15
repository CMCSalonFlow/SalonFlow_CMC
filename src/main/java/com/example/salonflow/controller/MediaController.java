package com.example.salonflow.controller;

import com.example.salonflow.dto.Upload.UploadResponse;
import com.example.salonflow.services.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public UploadResponse upload(
            @RequestParam("file") MultipartFile file
    ) {
        return mediaService.upload(file);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        mediaService.delete(id);
    }
}
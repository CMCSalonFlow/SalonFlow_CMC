package com.example.salonflow.ai.dto.hair;

public record HairVisionAnalysisRequest(
        Long mediaId,
        String imageDataUrl,
        String mimeType,
        String originalFileName,
        Long fileSize
) {
}

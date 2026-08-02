package com.example.salonflow.ai.dto.hair;

public record HairVisionAnalysisRequest(
        Long mediaId,
        String imageUrl,
        String mimeType,
        String originalFileName
) {
}

package com.example.salonflow.ai.dto.hair;

public record HairStyleImageSelectionResult(
        Long styleId,
        HairStyleImageResponse selectedImage,
        String selectionReason
) {
}

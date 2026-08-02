package com.example.salonflow.ai.dto.hair;

public record HairStyleConfirmRequest(
        Long analysisResultId,
        Long styleId,
        Long styleImageId
) {
}

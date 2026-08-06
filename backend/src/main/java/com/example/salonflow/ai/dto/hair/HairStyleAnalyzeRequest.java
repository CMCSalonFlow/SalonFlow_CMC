package com.example.salonflow.ai.dto.hair;

import com.example.salonflow.entity.enums.hair.HairGender;

import jakarta.validation.constraints.NotNull;

public record HairStyleAnalyzeRequest(
        @NotNull Long mediaId,
        @NotNull HairGender gender
) {
}

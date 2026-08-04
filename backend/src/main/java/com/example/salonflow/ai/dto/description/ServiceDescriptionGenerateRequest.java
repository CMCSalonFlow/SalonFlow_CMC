package com.example.salonflow.ai.dto.description;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ServiceDescriptionGenerateRequest(
        Long salonId,
        @NotBlank String serviceName,
        @Size(min = 3, max = 5)
        @NotEmpty List<@NotBlank String> keywords
) {
}

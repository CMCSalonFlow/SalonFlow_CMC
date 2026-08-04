package com.example.salonflow.ai.dto.description;

import java.time.Instant;
import java.util.List;

public record ServiceDescriptionGenerateResponse(
        Long salonId,
        String serviceName,
        List<String> keywords,
        String generatedDescription,
        String provider,
        String model,
        Integer promptVersion,
        Integer usedToday,
        Integer dailyLimit,
        Integer remainingToday,
        Instant generatedAt
) {
}

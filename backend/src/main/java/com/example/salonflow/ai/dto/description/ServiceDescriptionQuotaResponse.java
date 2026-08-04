package com.example.salonflow.ai.dto.description;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ServiceDescriptionQuotaResponse(
        Long salonId,
        LocalDate quotaDate,
        Integer usedToday,
        Integer dailyLimit,
        Integer remainingToday,
        LocalDateTime resetAt
) {
}

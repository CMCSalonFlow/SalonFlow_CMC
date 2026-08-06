package com.example.salonflow.dto.recommendation;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    private Long userId;
    private String abGroup;
    private String algorithmUsed;
    private LocalDateTime cachedAt;
    private List<ServiceRecommendationDto> recommendations;
}

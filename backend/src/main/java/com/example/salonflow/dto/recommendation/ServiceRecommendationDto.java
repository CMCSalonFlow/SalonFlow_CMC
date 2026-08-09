package com.example.salonflow.dto.recommendation;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRecommendationDto {

    private Long serviceId;
    private String name;
    private BigDecimal price;
    private Integer durationMinutes;
    private String description;
    private String categoryName;
    private Long branchId;
    private String branchName;
    private String imageUrl;
    private Double score;
    private String reason;
}

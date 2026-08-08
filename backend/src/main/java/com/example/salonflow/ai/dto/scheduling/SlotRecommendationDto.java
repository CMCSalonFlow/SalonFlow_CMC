package com.example.salonflow.ai.dto.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotRecommendationDto {

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Long staffId;
    private String staffName;
    private String staffAvatar;
    private String staffSpecialties;

    // Tổng điểm đề xuất (thang điểm 0 - 100)
    private Double totalScore;

    // Điểm chi tiết thành phần (thang 0.0 - 1.0)
    private Double workloadBalanceScore;
    private Double travelGapScore;
    private Double serviceFitScore;

    // Giải thích tự nhiên bằng Tiếng Việt lý do gợi ý slot này
    private String explanation;
}

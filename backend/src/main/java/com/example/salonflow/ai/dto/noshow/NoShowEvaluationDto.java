package com.example.salonflow.ai.dto.noshow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoShowEvaluationDto {

    private Long id;
    private LocalDate evaluationDate;
    private LocalDate startDate;
    private LocalDate endDate;

    private Integer totalEvaluatedBookings;
    private Integer truePositives;
    private Integer falsePositives;
    private Integer trueNegatives;
    private Integer falseNegatives;

    private Double accuracy;
    private Double accuracyPercentage;
    private Double precisionScore;
    private Double recallScore;
    private Double f1Score;

    private String notes;
}

package com.example.salonflow.dto.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffLowRatingWarningDto {

    private Long staffId;
    private String staffName;
    private String avatarUrl;
    private Long branchId;
    private String branchName;
    private Double thirtyDaysAvgRating;
    private Long thirtyDaysReviewCount;
    private String warningMessage;
}

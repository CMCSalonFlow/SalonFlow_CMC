package com.example.salonflow.dto.reviewanalytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingTrendResponse {

    private Long salonId;

    private Long branchId;

    private String fromMonth;

    private String toMonth;

    private List<RatingTrendPointResponse> points;
}

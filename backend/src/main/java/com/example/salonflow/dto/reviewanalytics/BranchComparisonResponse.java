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
public class BranchComparisonResponse {

    private Long salonId;

    private List<BranchComparisonItemResponse> branches;
}

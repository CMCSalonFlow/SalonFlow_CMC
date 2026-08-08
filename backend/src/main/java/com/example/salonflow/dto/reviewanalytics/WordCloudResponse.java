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
public class WordCloudResponse {

    private Long salonId;

    private Long branchId;

    /** Định dạng "YYYY-MM" */
    private String yearMonth;

    private List<KeywordFrequencyResponse> keywords;
}

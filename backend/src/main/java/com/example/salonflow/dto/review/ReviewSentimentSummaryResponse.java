package com.example.salonflow.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSentimentSummaryResponse {

    private long total;
    private long pending;
    private long processing;
    private long completed;
    private long failed;
    private long positive;
    private long neutral;
    private long negative;
}

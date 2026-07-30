package com.example.salonflow.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAdminItemResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long branchId;
    private String branchName;
    private Integer rating;
    private String sentiment;
    private BigDecimal sentimentConfidence;
    private String sentimentStatus;
    private String title;
    private String content;
    private String comment;
    private String ownerReply;
    private Instant createdAt;
}

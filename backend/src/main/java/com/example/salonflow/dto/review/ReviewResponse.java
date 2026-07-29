package com.example.salonflow.dto.review;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;

    private Long bookingId;

    private Long customerId;

    private String customerName;

    private String customerAvatar;

    private Long salonId;

    private Long branchId;

    private String branchName;

    private Integer rating;

    private String comment;

    private List<String> photos;

    private String ownerReply;

    private Instant ownerRepliedAt;

    private Boolean isHidden;

    private Instant hiddenAt;

    private String hiddenReason;

    private Instant createdAt;
}

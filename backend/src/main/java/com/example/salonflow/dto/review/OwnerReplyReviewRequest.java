package com.example.salonflow.dto.review;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerReplyReviewRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String replyContent;
}

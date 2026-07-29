package com.example.salonflow.dto.review;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportReviewRequest {

    @NotBlank(message = "Lý do báo cáo vi phạm không được để trống")
    private String reason;
}

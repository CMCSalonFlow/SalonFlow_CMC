package com.example.salonflow.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequest {

    @NotNull(message = "Số sao đánh giá không được để trống")
    @Min(value = 1, message = "Số sao đánh giá tối thiểu là 1")
    @Max(value = 5, message = "Số sao đánh giá tối đa là 5")
    private Integer rating;

    @Size(max = 200, message = "Tiêu đề nhận xét tối đa 200 ký tự")
    private String title;

    @Size(max = 1000, message = "Nội dung nhận xét tối đa 1000 ký tự")
    private String comment;

    @Size(max = 5, message = "Tối đa 5 hình ảnh đánh giá")
    private List<String> photos;
}

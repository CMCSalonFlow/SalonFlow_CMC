package com.example.salonflow.dto.review;

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
public class ReviewPageResponse {

    private List<ReviewAdminItemResponse> items;
    private long totalItems;
    private int totalPages;
    private int page;
    private int size;
}

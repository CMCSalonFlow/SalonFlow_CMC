package com.example.salonflow.dto.category;

import lombok.Data;

@Data
public class UpdateCategoryRequest {

    private String name;
    private Long iconMediaId;
    private String description;
    private Long salonId;
}
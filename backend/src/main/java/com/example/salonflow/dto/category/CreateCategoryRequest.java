package com.example.salonflow.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;

    private String icon;
    private String color;
    private String description;
}
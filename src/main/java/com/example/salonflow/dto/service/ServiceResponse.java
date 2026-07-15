package com.example.salonflow.dto.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ServiceResponse {

    private Long id;

    private Long branchId;

    private Long categoryId;

    private String categoryName;

    private String name;

    private BigDecimal price;

    private Integer durationMinutes;

    private String description;

    private Boolean depositRequired;

    private BigDecimal depositPercentage;
    
    private Boolean isActive;

    private List<String> images;
}
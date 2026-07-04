package com.example.salonflow.search.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BranchSearchRequest {

    /**
     * Hair Cut
     * Nail
     * ...
     */
    private String q;

    /**
     * service id
     */
    private Long serviceId;

    private BigDecimal priceMin;

    private BigDecimal priceMax;

    /**
     * Chưa dùng nhưng để sẵn
     */
    private Double ratingMin;

    /**
     * Geo search
     */
    private Double latitude;

    private Double longitude;

    /**
     * km
     */
    @Builder.Default
    private Double radius = 20d;

    /**
     * Search After
     */
    private String cursor;

    @Builder.Default
    private Integer size = 20;

}
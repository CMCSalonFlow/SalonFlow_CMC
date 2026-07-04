package com.example.salonflow.search.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BranchSearchItem {

    private Long branchId;

    private Long salonId;

    private String salonName;

    private String branchName;

    private String address;

    private Double latitude;

    private Double longitude;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Double rating;

    /**
     * km
     */
    private Double distance;

}
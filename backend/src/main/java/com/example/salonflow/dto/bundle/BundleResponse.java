package com.example.salonflow.dto.bundle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleResponse {

    private Long id;

    private Long branchId;

    private String name;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer totalDurationMinutes;

    private Boolean isActive;

    private List<BundleItemResponse> items;
}

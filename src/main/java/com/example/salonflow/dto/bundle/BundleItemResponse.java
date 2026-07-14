package com.example.salonflow.dto.bundle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleItemResponse {

    private Long serviceId;

    private String name;

    private BigDecimal price;

    private Integer durationMinutes;

    private Integer displayOrder;
}

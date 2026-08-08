package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRevenueBreakdownDto {
    private Long serviceId;
    private String serviceName;
    private String categoryName;
    private BigDecimal revenue;
    private Long itemCount;
    private Double percentage; // % tỷ trọng đóng góp doanh thu
}

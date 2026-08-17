package com.example.salonflow.dto.Salon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbySalonBranchResponse {
    private Long branchId;
    private String branchName;
    private String branchPhone;
    private String branchEmail;
    private String address;
    private Double latitude;
    private Double longitude;

    private Long salonId;
    private String salonName;
    private String salonDescription;
    private String logoUrl;

    private Double distanceMeters;
    private Double distanceKm;

    private BigDecimal ratingAverage;
    private Integer ratingCount;
    private Boolean isOpen;
}

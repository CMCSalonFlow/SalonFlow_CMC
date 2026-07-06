package com.example.salonflow.dto.Branch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchSummaryResponse {

    private Long id;

    private String name;

    private String address;

    private Double latitude;

    private Double longitude;

    private Boolean isActive;
}
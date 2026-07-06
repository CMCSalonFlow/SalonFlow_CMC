package com.example.salonflow.dto.Branch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchResponse {

    private Long id;

    private String name;

    private Long salonId;

    private String description;

    private String phone;

    private String email;

    private String address;

    private Double latitude;

    private Double longitude;

    private Boolean isActive;

    private java.util.List<BranchHourResponse> hours;
}
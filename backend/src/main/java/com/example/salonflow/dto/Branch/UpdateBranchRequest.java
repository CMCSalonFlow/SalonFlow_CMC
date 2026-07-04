package com.example.salonflow.dto.Branch;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateBranchRequest {

    @NotBlank
    private String name;

    private String phone;

    private String email;

    @NotBlank
    private String address;

    private Double latitude;

    private Double longitude;

    private Boolean isActive;
}

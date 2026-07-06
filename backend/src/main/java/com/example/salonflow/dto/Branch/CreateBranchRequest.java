package com.example.salonflow.dto.Branch;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBranchRequest {

    @NotBlank
    private String name;

    private String phone;

    private String email;

    @NotBlank
    private String address;

    private Double latitude;

    private Double longitude;

    @jakarta.validation.Valid
    private java.util.List<BranchHourRequest> hours;

}

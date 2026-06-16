package com.example.salonflow.dto.Salon;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSalonRequest {

    @NotBlank
    private String name;

    private String description;

    private String logoUrl;

    private String phone;

    private String email;

    private String website;
}
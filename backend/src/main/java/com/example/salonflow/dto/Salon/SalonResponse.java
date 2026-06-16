package com.example.salonflow.dto.Salon;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalonResponse {

    private Long id;

    private String name;

    private String description;

    private String logoUrl;

    private String phone;

    private String email;

    private String website;
}
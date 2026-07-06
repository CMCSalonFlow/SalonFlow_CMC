package com.example.salonflow.dto.Salon;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonResponse {

    private Long id;

    private String name;

    private String description;

    private String phone;

    private String email;

    private String website;

    private String logoUrl;



    private List<SalonPhotoResponse> photos;
}
package com.example.salonflow.dto.Salon;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonPhotoResponse {

    private String url;

    private Boolean isPrimary;
}
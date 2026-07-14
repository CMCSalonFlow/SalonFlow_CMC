package com.example.salonflow.dto.Salon;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonPhotoResponse {

    private Long mediaId;

    private String url;

    private Boolean isPrimary;
}
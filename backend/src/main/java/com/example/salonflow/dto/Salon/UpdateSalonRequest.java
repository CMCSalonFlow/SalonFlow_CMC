package com.example.salonflow.dto.Salon;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSalonRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String address;

    private String phone;

    @Email
    private String email;

    private String website;

    @Valid
    private List<SalonHourRequest> hours;

    private List<String> photos;
}
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

    private String phone;

    @Email
    private String email;

    private String website;



    private Long logoMediaId;

    private List<Long> photoMediaIds;
}
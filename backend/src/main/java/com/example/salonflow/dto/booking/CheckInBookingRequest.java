package com.example.salonflow.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CheckInBookingRequest {

    @NotBlank(message = "Signature không được để trống")
    private String signature;
}

package com.example.salonflow.dto.Salon;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectSalonRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String reason;
}

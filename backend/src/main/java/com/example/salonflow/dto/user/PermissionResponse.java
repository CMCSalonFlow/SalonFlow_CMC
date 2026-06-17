package com.example.salonflow.dto.user;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Builder
public class PermissionResponse {

    private Long id;

    private String code;

    private String description;
}

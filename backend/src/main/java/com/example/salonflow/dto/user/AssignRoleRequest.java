package com.example.salonflow.dto.user;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
public class AssignRoleRequest {

    private Long roleId;
}

package com.example.salonflow.dto.user;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Builder;

@Getter
@Builder
public class RoleResponse {

    private Long id;

    private String name;

    private String description;

    private List<PermissionResponse> permissions;
}

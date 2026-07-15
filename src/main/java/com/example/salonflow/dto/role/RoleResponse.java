package com.example.salonflow.dto.role;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class RoleResponse {
    private Long id;
    private String code;
    private String name;
    private String description;

    private Set<String> permissions;
}
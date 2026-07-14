package com.example.salonflow.dto.role;

import lombok.Data;

import java.util.Set;

@Data
public class RoleCreateRequest {
    private String code;
    private String name;
    private String description;

    private Set<Long> permissionIds;
}
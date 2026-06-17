package com.example.salonflow.services.service;

import com.example.salonflow.dto.user.CreateRoleRequest;
import com.example.salonflow.dto.user.RoleResponse;
import com.example.salonflow.dto.user.UpdateRoleRequest;

import java.util.List;

public interface RoleService {

    RoleResponse createRole(
            CreateRoleRequest request
    );

    RoleResponse updateRole(
            Long roleId,
            UpdateRoleRequest request
    );

    RoleResponse getRoleById(
            Long roleId
    );

    List<RoleResponse> getAllRoles();

    void deleteRole(
            Long roleId
    );
}
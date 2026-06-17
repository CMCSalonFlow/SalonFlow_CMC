package com.example.salonflow.services.service;

import com.example.salonflow.dto.role.*;

import java.util.List;

public interface RoleService {

    RoleResponse createRole(RoleCreateRequest request);

    RoleResponse updateRole(Long roleId, RoleUpdateRequest request);

    void deleteRole(Long roleId);

    RoleResponse getRoleById(Long roleId);

    List<RoleResponse> getAllRoles();

    void assignPermissions(Long roleId, java.util.Set<Long> permissionIds);
}
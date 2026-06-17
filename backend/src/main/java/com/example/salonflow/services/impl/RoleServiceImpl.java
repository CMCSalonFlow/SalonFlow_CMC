package com.example.salonflow.services.impl;

import com.example.salonflow.dto.role.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public RoleResponse createRole(RoleCreateRequest request) {

        Role role = Role.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Role saved = roleRepository.save(role);

        if (request.getPermissionIds() != null) {
            assignPermissions(saved.getId(), request.getPermissionIds());
        }

        return mapToResponse(saved);
    }

    @Override
    public RoleResponse updateRole(Long roleId, RoleUpdateRequest request) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        return mapToResponse(roleRepository.save(role));
    }

    @Override
    public void deleteRole(Long roleId) {
        roleRepository.deleteById(roleId);
    }

    @Override
    public RoleResponse getRoleById(Long roleId) {
        return roleRepository.findById(roleId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    @Override
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void assignPermissions(Long roleId, Set<Long> permissionIds) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);

        for (Permission p : permissions) {

            RolePermissionId id = new RolePermissionId(roleId, p.getId());

            if (rolePermissionRepository.existsById(id)) continue;

            RolePermission rp = RolePermission.builder()
                    .id(id)
                    .role(role)
                    .permission(p)
                    .build();

            rolePermissionRepository.save(rp);
        }
    }

    private RoleResponse mapToResponse(Role role) {

        Set<String> perms = role.getRolePermissions()
                .stream()
                .map(rp -> rp.getPermission().getCode())
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(perms)
                .build();
    }
}
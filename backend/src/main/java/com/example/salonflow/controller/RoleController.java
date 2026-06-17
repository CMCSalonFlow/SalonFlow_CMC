package com.example.salonflow.controller;

import com.example.salonflow.dto.role.*;
import com.example.salonflow.services.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // CREATE ROLE
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(
            @RequestBody RoleCreateRequest request
    ) {
        return ResponseEntity.ok(roleService.createRole(request));
    }

    // GET ALL ROLES
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    // GET ROLE BY ID
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    // UPDATE ROLE
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest request
    ) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    // DELETE ROLE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable Long id
    ) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    // ASSIGN PERMISSIONS TO ROLE
    @PostMapping("/{id}/permissions")
    public ResponseEntity<Void> assignPermissions(
            @PathVariable Long id,
            @RequestBody Set<Long> permissionIds
    ) {
        roleService.assignPermissions(id, permissionIds);
        return ResponseEntity.ok().build();
    }
}
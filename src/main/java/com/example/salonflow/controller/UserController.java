package com.example.salonflow.controller;

import com.example.salonflow.dto.user.*;
import com.example.salonflow.services.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // CREATE USER
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestBody UserCreateRequest request
    ) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    // GET ALL USERS
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // GET USER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // UPDATE USER
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // DELETE USER
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ASSIGN ROLES TO USER
    @PostMapping("/{id}/roles")
    public ResponseEntity<Void> assignRoles(
            @PathVariable Long id,
            @RequestBody Set<Long> roleIds
    ) {
        userService.assignRolesToUser(id, roleIds);
        return ResponseEntity.ok().build();
    }

    // REMOVE ROLE FROM USER
    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> removeRole(
            @PathVariable Long id,
            @PathVariable Long roleId
    ) {
        userService.removeRoleFromUser(id, roleId);
        return ResponseEntity.noContent().build();
    }
}
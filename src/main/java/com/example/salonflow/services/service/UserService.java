package com.example.salonflow.services.service;

import com.example.salonflow.dto.user.*;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse updateUser(Long userId, UserUpdateRequest request);

    void deleteUser(Long userId);

    UserResponse getUserById(Long userId);

    List<UserResponse> getAllUsers();

    void assignRolesToUser(Long userId, java.util.Set<Long> roleIds);

    void removeRoleFromUser(Long userId, Long roleId);
}
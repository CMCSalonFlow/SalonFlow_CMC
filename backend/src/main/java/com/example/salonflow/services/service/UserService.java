package com.example.salonflow.services.service;

import com.example.salonflow.dto.User.CreateUserRequest;
import com.example.salonflow.dto.User.UpdateUserRequest;
import com.example.salonflow.dto.User.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse createUser(
            CreateUserRequest request
    );

    UserResponse updateUser(
            Long userId,
            UpdateUserRequest request
    );

    UserResponse getUserById(
            Long userId
    );

    Page<UserResponse> getAllUsers(
            Pageable pageable
    );

    void deleteUser(
            Long userId
    );
}
package com.example.salonflow.services.impl;

import com.example.salonflow.dto.user.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.UserStatus;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(UserCreateRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);

        // assign roles nếu có
        if (request.getRoleIds() != null) {
            assignRolesToUser(saved.getId(), request.getRoleIds());
        }

        return mapToResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());

        return mapToResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void assignRolesToUser(Long userId, Set<Long> roleIds) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Role> roles = roleRepository.findAllById(roleIds);

        for (Role role : roles) {

            UserRoleId id = new UserRoleId(user.getId(), role.getId());

            if (userRoleRepository.existsById(id)) continue;

            UserRole userRole = UserRole.builder()
                    .id(id)
                    .user(user)
                    .role(role)
                    .assignedAt(LocalDateTime.now())
                    .build();

            userRoleRepository.save(userRole);
        }
    }

    @Override
    public void removeRoleFromUser(Long userId, Long roleId) {
        userRoleRepository.deleteById(new UserRoleId(userId, roleId));
    }

    private UserResponse mapToResponse(User user) {

    Set<String> roles = user.getUserRoles()
            .stream()
            .map((UserRole ur) -> ur.getRole().getCode())
            .collect(Collectors.toSet());
            
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .roles(roles)
                .build();
    }
}
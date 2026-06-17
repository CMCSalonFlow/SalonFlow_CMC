package com.example.salonflow.dto.user;
import java.util.List;

import com.example.salonflow.entity.enums.UserStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.Builder;

@Getter
@Builder
public class UserResponse {

    private Long id;

    private String username;

    private String email;

    private String fullName;

    private String phone;

    private String avatarUrl;

    private UserStatus status;

    private List<RoleResponse> roles;
}
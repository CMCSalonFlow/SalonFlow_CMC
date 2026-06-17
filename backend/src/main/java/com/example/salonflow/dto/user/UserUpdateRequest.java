package com.example.salonflow.dto.user;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String fullName;
    private String phone;
    private String avatarUrl;
}
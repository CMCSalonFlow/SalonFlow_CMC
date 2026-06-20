package com.example.salonflow.dto.user;

import lombok.Data;

import java.util.Set;

@Data
public class UserCreateRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phone;

    private Set<Long> roleIds;
}
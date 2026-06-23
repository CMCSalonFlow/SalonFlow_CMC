package com.example.salonflow.dto.user;

import lombok.Data;
import java.util.Set;
@Data
public class UserUpdateRequest {
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Set<Long> roleIds;
}
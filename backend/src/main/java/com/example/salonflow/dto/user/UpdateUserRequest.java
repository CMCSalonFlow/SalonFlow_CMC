package com.example.salonflow.dto.user;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import com.example.salonflow.entity.enums.UserStatus;
@Getter
@Setter
public class UpdateUserRequest {

    private String fullName;

    private String phone;

    private String avatarUrl;

    private UserStatus status;
}
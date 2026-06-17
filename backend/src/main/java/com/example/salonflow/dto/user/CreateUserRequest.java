package com.example.salonflow.dto.user;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
public class CreateUserRequest {

    private String username;

    private String email;

    private String password;

    private String fullName;

    private String phone;

    private Set<Long> roleIds;
}

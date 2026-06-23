package com.example.salonflow.dto.Branch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInBranchResponse {

    private Long id;

    private String fullName;

    private String email;

    private String phone;
}
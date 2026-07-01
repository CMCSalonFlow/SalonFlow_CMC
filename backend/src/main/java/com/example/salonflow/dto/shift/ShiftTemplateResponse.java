package com.example.salonflow.dto.shift;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ShiftTemplateResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long branchId;
    private String branchName;
    private String name;
    private String description;
    private Boolean isActive;
    private List<ShiftTemplateDetailResponse> details;
}

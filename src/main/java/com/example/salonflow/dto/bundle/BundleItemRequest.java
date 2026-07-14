package com.example.salonflow.dto.bundle;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleItemRequest {

    @NotNull(message = "ID dịch vụ không được để trống")
    private Long serviceId;

    @Builder.Default
    private Integer displayOrder = 0;
}

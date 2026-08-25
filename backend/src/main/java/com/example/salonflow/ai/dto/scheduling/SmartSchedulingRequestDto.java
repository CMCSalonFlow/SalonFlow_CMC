package com.example.salonflow.ai.dto.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSchedulingRequestDto {
    private Long branchId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private List<Long> serviceIds;
    private Long bundleId;
    private Long preferredStaffId;
    private Long customerId;
    private Double userLat;
    private Double userLng;
}

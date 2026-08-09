package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeakHourHeatmapResponse {
    private Long salonId;
    private Long branchId;
    private long totalBookingsAnalysed;
    private long maxBookingCount;
    private String busiestDay;       // VD: "Thứ 7"
    private String busiestHour;      // VD: "14:00 - 15:00"
    private List<PeakHourCellDto> matrix;
}

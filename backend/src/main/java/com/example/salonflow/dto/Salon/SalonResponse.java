package com.example.salonflow.dto.Salon;

import com.example.salonflow.entity.SalonStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonResponse {

    private Long id;

    private String name;

    private String description;

    private String phone;

    private String email;

    private String website;

    private String logoUrl;

    private SalonStatus status;

    private String rejectionReason;

    private LocalDateTime rejectedAt;

    private LocalDateTime approvedAt;

    private Boolean canAppeal;

    private Long daysUntilAppeal;

    private List<SalonPhotoResponse> photos;
}

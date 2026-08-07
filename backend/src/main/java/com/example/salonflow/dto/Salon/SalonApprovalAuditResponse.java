package com.example.salonflow.dto.Salon;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonApprovalAuditResponse {
    private Long id;
    private Long salonId;
    private String salonName;
    private Long adminId;
    private String adminName;
    private String adminEmail;
    private String action;
    private String reason;
    private Instant createdAt;
}

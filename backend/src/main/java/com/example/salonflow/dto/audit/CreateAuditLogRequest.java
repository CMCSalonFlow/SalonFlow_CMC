package com.example.salonflow.dto.audit;

import com.example.salonflow.entity.enums.AuditAction;
import lombok.*;

/**
 * DTO nội bộ - dùng khi các service khác gọi AuditLogService.log(...)
 * để ghi log. Không phải request nhận trực tiếp từ client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAuditLogRequest {
    private Long userId;
    private String userEmail;
    private AuditAction action;
    private String resourceType;
    private String resourceId;
    private Object oldValue;
    private Object newValue;
    private String ipAddress;
    private String userAgent;
}

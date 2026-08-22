package com.example.salonflow.services.impl;

import com.example.salonflow.dto.audit.AuditLogResponse;
import com.example.salonflow.dto.audit.CreateAuditLogRequest;
import com.example.salonflow.entity.AuditLog;
import com.example.salonflow.entity.enums.AuditAction;
import com.example.salonflow.repository.AuditLogRepository;
import com.example.salonflow.services.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async("auditTaskExecutor")
    public void log(CreateAuditLogRequest request) {
        try {
            AuditLog entity = AuditLog.builder()
                    .userId(request.getUserId())
                    .userEmail(request.getUserEmail())
                    .action(request.getAction())
                    .resourceType(request.getResourceType())
                    .resourceId(request.getResourceId())
                    .oldValue(toJson(request.getOldValue()))
                    .newValue(toJson(request.getNewValue()))
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .build();

            auditLogRepository.save(entity);
        } catch (Exception e) {
            // Không để lỗi ghi audit log làm fail nghiệp vụ chính
            log.error("Failed to write audit log: action={}, resourceType={}, resourceId={}",
                    request.getAction(), request.getResourceType(), request.getResourceId(), e);
        }
    }

    @Override
    public Page<AuditLogResponse> search(
            Long userId,
            AuditAction action,
            String resourceType,
            String resourceId,
            OffsetDateTime from,
            OffsetDateTime to,
            String search,
            Long scopedToOwnerId,
            String ownedSalonId,
            Pageable pageable
    ) {
        // Postgres không thể suy luận kiểu dữ liệu cho tham số chỉ dùng trong
        // "IS NULL" check khi giá trị null. Thay vì để null xuống query,
        // điền sẵn khoảng thời gian rộng nhất có thể khi người dùng không lọc theo ngày.
        OffsetDateTime effectiveFrom = from != null ? from : OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime effectiveTo = to != null ? to : OffsetDateTime.now().plusYears(100);

        return auditLogRepository
                .searchAuditLogs(userId, action, resourceType, resourceId, effectiveFrom, effectiveTo, search,
                        scopedToOwnerId, ownedSalonId, pageable)
                .map(this::toResponse);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return value instanceof String ? (String) value : objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize audit log value to JSON", e);
            return null;
        }
    }

    private AuditLogResponse toResponse(AuditLog entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .userEmail(entity.getUserEmail())
                .action(entity.getAction() != null ? entity.getAction().name() : null)
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
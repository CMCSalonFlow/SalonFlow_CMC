package com.example.salonflow.services.service;

import com.example.salonflow.dto.audit.AuditLogResponse;
import com.example.salonflow.dto.audit.CreateAuditLogRequest;
import com.example.salonflow.entity.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

public interface AuditLogService {

    /**
     * Ghi audit log. Chạy async (executor riêng "auditTaskExecutor"),
     * không throw exception ra ngoài để không làm fail nghiệp vụ chính
     * nếu ghi log lỗi.
     */
    void log(CreateAuditLogRequest request);

    /**
     * Tìm kiếm / lọc audit log cho màn hình admin.
     *
     * @param scopedToOwnerId null nếu SUPER_ADMIN (không giới hạn phạm vi);
     *                        userId của SALON_OWNER nếu cần giới hạn phạm vi.
     * @param ownedSalonId    id salon mà SALON_OWNER sở hữu (null nếu SUPER_ADMIN).
     */
    Page<AuditLogResponse> search(
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
    );
}

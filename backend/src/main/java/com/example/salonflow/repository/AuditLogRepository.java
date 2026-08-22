package com.example.salonflow.repository;

import com.example.salonflow.entity.AuditLog;
import com.example.salonflow.entity.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog auditLog);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:userId IS NULL OR a.userId = :userId)
          AND (:action IS NULL OR a.action = :action)
          AND (:resourceType IS NULL OR a.resourceType = :resourceType)
          AND (:resourceId IS NULL OR a.resourceId = :resourceId)
          AND (a.createdAt >= :from)
          AND (a.createdAt <= :to)
          AND (:search IS NULL
               OR LOWER(a.userEmail) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
               OR LOWER(a.resourceType) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
               OR LOWER(a.resourceId) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
          AND (
                :scopedToOwnerId IS NULL
                OR a.userId = :scopedToOwnerId
                OR (a.resourceType IN ('salon','branch') AND a.resourceId = :ownedSalonId)
              )
        """)
    Page<AuditLog> searchAuditLogs(
            @Param("userId") Long userId,
            @Param("action") AuditAction action,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("search") String search,
            @Param("scopedToOwnerId") Long scopedToOwnerId,
            @Param("ownedSalonId") String ownedSalonId,
            Pageable pageable
    );
}
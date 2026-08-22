package com.example.salonflow.controller;

import com.example.salonflow.dto.audit.AuditLogResponse;
import com.example.salonflow.entity.enums.AuditAction;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.security.CustomUserPrincipal;
import com.example.salonflow.services.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SALON_OWNER')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;
    private final SalonRepository salonRepository;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> search(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        boolean isSuperAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        // SALON_OWNER chỉ xem log do chính họ tạo ra hoặc log liên quan tới
        // salon họ sở hữu. SUPER_ADMIN xem toàn bộ, không giới hạn.
        Long scopedOwnerId = isSuperAdmin ? null : principal.getId();
        String ownedSalonId = isSuperAdmin
                ? null
                : salonRepository.findFirstByOwnerId(principal.getId())
                        .map(salon -> String.valueOf(salon.getId()))
                        .orElse("-1"); // chưa có salon nào -> không match gì cả

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogResponse> result = auditLogService.search(
                userId, action, resourceType, resourceId, from, to, search,
                scopedOwnerId, ownedSalonId, pageable
        );
        return ResponseEntity.ok(result);
    }
}

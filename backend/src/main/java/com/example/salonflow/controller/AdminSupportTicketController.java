package com.example.salonflow.controller;

import com.example.salonflow.dto.support.*;
import com.example.salonflow.entity.enums.TicketCategory;
import com.example.salonflow.entity.enums.TicketPriority;
import com.example.salonflow.entity.enums.TicketStatus;
import com.example.salonflow.security.CustomUserPrincipal;
import com.example.salonflow.services.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/support/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    @GetMapping
    public ResponseEntity<Page<SupportTicketResponse>> getAdminTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) Boolean slaBreached,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SupportTicketResponse> tickets = supportTicketService.getAdminTickets(status, priority, category, slaBreached, assignedToId, search, pageable);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/kpi-stats")
    public ResponseEntity<Map<String, Object>> getKpiStats() {
        Map<String, Object> stats = supportTicketService.getTicketKpiStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupportTicketDetailResponse> getAdminTicketDetails(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id
    ) {
        SupportTicketDetailResponse response = supportTicketService.getTicketDetails(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<SupportTicketResponse> updateStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        SupportTicketResponse response = supportTicketService.updateStatus(id, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<SupportTicketResponse> assignTicket(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request
    ) {
        SupportTicketResponse response = supportTicketService.assignTicket(id, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/replies")
    public ResponseEntity<SupportTicketReplyResponse> addAdminReply(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AddTicketReplyRequest request
    ) {
        SupportTicketReplyResponse response = supportTicketService.addReply(id, principal.getId(), request);
        return ResponseEntity.ok(response);
    }
}

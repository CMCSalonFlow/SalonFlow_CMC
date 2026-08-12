package com.example.salonflow.controller;

import com.example.salonflow.dto.support.*;
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

@RestController
@RequestMapping("/api/v1/support/tickets")
@RequiredArgsConstructor
public class HelpCenterController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicketResponse> createTicket(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateTicketRequest request
    ) {
        SupportTicketResponse response = supportTicketService.createTicket(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<SupportTicketResponse>> getUserTickets(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SupportTicketResponse> tickets = supportTicketService.getUserTickets(principal.getId(), status, pageable);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicketDetailResponse> getTicketDetails(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id
    ) {
        SupportTicketDetailResponse response = supportTicketService.getTicketDetails(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/replies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicketReplyResponse> addReply(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AddTicketReplyRequest request
    ) {
        SupportTicketReplyResponse response = supportTicketService.addReply(id, principal.getId(), request);
        return ResponseEntity.ok(response);
    }
}

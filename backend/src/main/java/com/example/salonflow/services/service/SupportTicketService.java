package com.example.salonflow.services.service;

import com.example.salonflow.dto.support.*;
import com.example.salonflow.entity.enums.TicketCategory;
import com.example.salonflow.entity.enums.TicketPriority;
import com.example.salonflow.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface SupportTicketService {

    SupportTicketResponse createTicket(Long currentUserId, CreateTicketRequest request);

    Page<SupportTicketResponse> getUserTickets(Long currentUserId, TicketStatus status, Pageable pageable);

    SupportTicketDetailResponse getTicketDetails(Long ticketId, Long currentUserId);

    SupportTicketReplyResponse addReply(Long ticketId, Long currentUserId, AddTicketReplyRequest request);

    // Admin operations
    Page<SupportTicketResponse> getAdminTickets(
            TicketStatus status,
            TicketPriority priority,
            TicketCategory category,
            Boolean slaBreached,
            Long assignedToId,
            String search,
            Pageable pageable
    );

    SupportTicketResponse updateStatus(Long ticketId, Long currentUserId, UpdateTicketStatusRequest request);

    SupportTicketResponse assignTicket(Long ticketId, Long currentUserId, AssignTicketRequest request);

    Map<String, Object> getTicketKpiStats();

    void scanAndMarkSlaBreaches();
}

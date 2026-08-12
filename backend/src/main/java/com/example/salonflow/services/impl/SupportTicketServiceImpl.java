package com.example.salonflow.services.impl;

import com.example.salonflow.dto.support.*;
import com.example.salonflow.entity.SupportTicket;
import com.example.salonflow.entity.SupportTicketReply;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.enums.TicketCategory;
import com.example.salonflow.entity.enums.TicketPriority;
import com.example.salonflow.entity.enums.TicketStatus;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.SupportTicketReplyRepository;
import com.example.salonflow.repository.SupportTicketRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.services.service.EmailService;
import com.example.salonflow.services.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    @Transactional
    public SupportTicketResponse createTicket(Long currentUserId, CreateTicketRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + currentUserId));

        String ticketCode = "TK-" + System.currentTimeMillis() % 1000000;
        int slaHours = request.getPriority().getSlaHours();
        OffsetDateTime slaDueAt = OffsetDateTime.now().plusHours(slaHours);

        SupportTicket ticket = SupportTicket.builder()
                .ticketCode(ticketCode)
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(TicketStatus.OPEN)
                .createdBy(user)
                .slaDueAt(slaDueAt)
                .slaBreached(false)
                .build();

        ticket = ticketRepository.save(ticket);
        log.info("Đã khởi tạo Support Ticket thành công: {} (Code: {}) cho User: {}", ticket.getId(), ticketCode, user.getEmail());

        // Send Email Notification
        try {
            emailService.sendTicketCreatedEmail(
                    user.getEmail(),
                    user.getFullName() != null ? user.getFullName() : user.getUsername(),
                    ticketCode,
                    ticket.getSubject(),
                    ticket.getPriority().getDescription(),
                    slaDueAt.format(DATE_FORMATTER)
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thông báo tạo ticket: {}", e.getMessage());
        }

        return mapToResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getUserTickets(Long currentUserId, TicketStatus status, Pageable pageable) {
        Page<SupportTicket> page;
        if (status != null) {
            page = ticketRepository.findByCreatedByIdAndStatus(currentUserId, status, pageable);
        } else {
            page = ticketRepository.findByCreatedById(currentUserId, pageable);
        }
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketDetailResponse getTicketDetails(Long ticketId, Long currentUserId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Support Ticket ID: " + ticketId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + currentUserId));

        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(ur -> "SUPER_ADMIN".equalsIgnoreCase(ur.getRole().getName()) || "ADMIN".equalsIgnoreCase(ur.getRole().getName()));

        if (!isAdmin && !ticket.getCreatedBy().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập Ticket này");
        }

        List<SupportTicketReply> replies;
        if (isAdmin) {
            replies = replyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        } else {
            replies = replyRepository.findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticketId);
        }

        List<SupportTicketReplyResponse> replyResponses = replies.stream()
                .map(this::mapToReplyResponse)
                .collect(Collectors.toList());

        return SupportTicketDetailResponse.builder()
                .ticket(mapToResponse(ticket))
                .replies(replyResponses)
                .build();
    }

    @Override
    @Transactional
    public SupportTicketReplyResponse addReply(Long ticketId, Long currentUserId, AddTicketReplyRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Support Ticket ID: " + ticketId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + currentUserId));

        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(ur -> "SUPER_ADMIN".equalsIgnoreCase(ur.getRole().getName()) || "ADMIN".equalsIgnoreCase(ur.getRole().getName()));

        if (!isAdmin && !ticket.getCreatedBy().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền gửi phản hồi trong Ticket này");
        }

        boolean isInternalNote = Boolean.TRUE.equals(request.getIsInternalNote());
        if (isInternalNote && !isAdmin) {
            throw new AccessDeniedException("Chỉ Ban Quản Trị Admin mới có thể gửi Ghi chú nội bộ");
        }

        SupportTicketReply reply = SupportTicketReply.builder()
                .ticket(ticket)
                .user(currentUser)
                .message(request.getMessage())
                .isInternalNote(isInternalNote)
                .createdAt(OffsetDateTime.now())
                .build();

        reply = replyRepository.save(reply);

        // Auto update status to IN_PROGRESS if admin replies and status is OPEN
        if (isAdmin && ticket.getStatus() == TicketStatus.OPEN && !isInternalNote) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        // Send Email Notification if not an internal note
        if (!isInternalNote) {
            try {
                if (isAdmin) {
                    // Send to ticket creator
                    emailService.sendTicketReplyNotificationEmail(
                            ticket.getCreatedBy().getEmail(),
                            ticket.getCreatedBy().getFullName() != null ? ticket.getCreatedBy().getFullName() : ticket.getCreatedBy().getUsername(),
                            ticket.getTicketCode(),
                            currentUser.getFullName() != null ? currentUser.getFullName() : "SalonFlow Support Admin",
                            request.getMessage()
                    );
                } else if (ticket.getAssignedTo() != null) {
                    // Send to assigned admin
                    emailService.sendTicketReplyNotificationEmail(
                            ticket.getAssignedTo().getEmail(),
                            ticket.getAssignedTo().getFullName() != null ? ticket.getAssignedTo().getFullName() : ticket.getAssignedTo().getUsername(),
                            ticket.getTicketCode(),
                            currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername(),
                            request.getMessage()
                    );
                }
            } catch (Exception e) {
                log.error("Lỗi khi gửi email thông báo reply ticket: {}", e.getMessage());
            }
        }

        return mapToReplyResponse(reply);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getAdminTickets(
            TicketStatus status,
            TicketPriority priority,
            TicketCategory category,
            Boolean slaBreached,
            Long assignedToId,
            String search,
            Pageable pageable
    ) {
        Page<SupportTicket> page = ticketRepository.findAdminTickets(status, priority, category, slaBreached, assignedToId, search, pageable);
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public SupportTicketResponse updateStatus(Long ticketId, Long currentUserId, UpdateTicketStatusRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Support Ticket ID: " + ticketId));

        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            return mapToResponse(ticket);
        }

        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(OffsetDateTime.now());
        } else if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(OffsetDateTime.now());
        }

        ticketRepository.save(ticket);
        log.info("Cập nhật trạng thái Ticket #{}: {} -> {}", ticket.getTicketCode(), oldStatus, newStatus);

        // Send Email Notification
        try {
            emailService.sendTicketStatusChangedEmail(
                    ticket.getCreatedBy().getEmail(),
                    ticket.getCreatedBy().getFullName() != null ? ticket.getCreatedBy().getFullName() : ticket.getCreatedBy().getUsername(),
                    ticket.getTicketCode(),
                    newStatus.getDescription()
            );
        } catch (Exception e) {
            log.error("Lỗi gửi email cập nhật trạng thái ticket: {}", e.getMessage());
        }

        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public SupportTicketResponse assignTicket(Long ticketId, Long currentUserId, AssignTicketRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Support Ticket ID: " + ticketId));

        User assignee = userRepository.findById(request.getAssigneeUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cán bộ xử lý ID: " + request.getAssigneeUserId()));

        ticket.setAssignedTo(assignee);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        ticketRepository.save(ticket);
        log.info("Đã phân công Ticket #{}: Cho Admin {}", ticket.getTicketCode(), assignee.getEmail());

        return mapToResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTicketKpiStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("openCount", ticketRepository.countByStatus(TicketStatus.OPEN));
        stats.put("inProgressCount", ticketRepository.countByStatus(TicketStatus.IN_PROGRESS));
        stats.put("resolvedCount", ticketRepository.countByStatus(TicketStatus.RESOLVED));
        stats.put("closedCount", ticketRepository.countByStatus(TicketStatus.CLOSED));
        stats.put("slaBreachedCount", ticketRepository.countBySlaBreachedTrueAndStatusNotIn(List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED)));
        return stats;
    }

    @Override
    @Transactional
    public void scanAndMarkSlaBreaches() {
        OffsetDateTime now = OffsetDateTime.now();
        List<SupportTicket> overdueTickets = ticketRepository.findOverdueOpenTickets(now);
        for (SupportTicket ticket : overdueTickets) {
            ticket.setSlaBreached(true);
            ticketRepository.save(ticket);
            log.warn("🚨 WARNING: Support Ticket #{}` (Priority {}) đã vi phạm thời hạn SLA!", ticket.getTicketCode(), ticket.getPriority());
        }
    }

    private SupportTicketResponse mapToResponse(SupportTicket ticket) {
        OffsetDateTime now = OffsetDateTime.now();
        long remainingMinutes = Duration.between(now, ticket.getSlaDueAt()).toMinutes();
        boolean isBreached = Boolean.TRUE.equals(ticket.getSlaBreached()) || (remainingMinutes < 0 && ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED);

        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .categoryName(ticket.getCategory().getDescription())
                .priority(ticket.getPriority())
                .priorityName(ticket.getPriority().getDescription())
                .slaHours(ticket.getPriority().getSlaHours())
                .status(ticket.getStatus())
                .statusName(ticket.getStatus().getDescription())
                .createdByUserId(ticket.getCreatedBy().getId())
                .createdByUserName(ticket.getCreatedBy().getFullName() != null ? ticket.getCreatedBy().getFullName() : ticket.getCreatedBy().getUsername())
                .createdByUserEmail(ticket.getCreatedBy().getEmail())
                .assignedToUserId(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null)
                .assignedToUserName(ticket.getAssignedTo() != null ? (ticket.getAssignedTo().getFullName() != null ? ticket.getAssignedTo().getFullName() : ticket.getAssignedTo().getUsername()) : "Chưa phân công")
                .slaDueAt(ticket.getSlaDueAt())
                .slaBreached(isBreached)
                .remainingMinutes(remainingMinutes)
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .createdAt(ticket.getCreatedAt() != null ? ticket.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(ticket.getUpdatedAt() != null ? ticket.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .build();
    }

    private SupportTicketReplyResponse mapToReplyResponse(SupportTicketReply reply) {
        User u = reply.getUser();
        boolean isAdmin = u.getUserRoles().stream()
                .anyMatch(ur -> "SUPER_ADMIN".equalsIgnoreCase(ur.getRole().getName()) || "ADMIN".equalsIgnoreCase(ur.getRole().getName()));

        return SupportTicketReplyResponse.builder()
                .id(reply.getId())
                .ticketId(reply.getTicket().getId())
                .userId(u.getId())
                .userName(u.getFullName() != null ? u.getFullName() : u.getUsername())
                .userEmail(u.getEmail())
                .userAvatar(u.getAvatarUrl())
                .isAdmin(isAdmin)
                .message(reply.getMessage())
                .isInternalNote(reply.getIsInternalNote())
                .createdAt(reply.getCreatedAt())
                .build();
    }
}

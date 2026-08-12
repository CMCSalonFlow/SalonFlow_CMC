package com.example.salonflow.dto.support;

import com.example.salonflow.entity.enums.TicketCategory;
import com.example.salonflow.entity.enums.TicketPriority;
import com.example.salonflow.entity.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketResponse {
    private Long id;
    private String ticketCode;
    private String subject;
    private String description;
    private TicketCategory category;
    private String categoryName;
    private TicketPriority priority;
    private String priorityName;
    private int slaHours;
    private TicketStatus status;
    private String statusName;

    private Long createdByUserId;
    private String createdByUserName;
    private String createdByUserEmail;

    private Long assignedToUserId;
    private String assignedToUserName;

    private OffsetDateTime slaDueAt;
    private Boolean slaBreached;
    private Long remainingMinutes;

    private OffsetDateTime resolvedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

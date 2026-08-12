package com.example.salonflow.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketReplyResponse {
    private Long id;
    private Long ticketId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userAvatar;
    private Boolean isAdmin;
    private String message;
    private Boolean isInternalNote;
    private OffsetDateTime createdAt;
}

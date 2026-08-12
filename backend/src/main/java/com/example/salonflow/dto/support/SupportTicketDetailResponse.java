package com.example.salonflow.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketDetailResponse {
    private SupportTicketResponse ticket;
    private List<SupportTicketReplyResponse> replies;
}

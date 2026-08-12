package com.example.salonflow.repository;

import com.example.salonflow.entity.SupportTicketReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketReplyRepository extends JpaRepository<SupportTicketReply, Long> {

    List<SupportTicketReply> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    List<SupportTicketReply> findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(Long ticketId);
}

package com.example.salonflow.repository;

import com.example.salonflow.entity.SupportTicket;
import com.example.salonflow.entity.enums.TicketCategory;
import com.example.salonflow.entity.enums.TicketPriority;
import com.example.salonflow.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByTicketCode(String ticketCode);

    Page<SupportTicket> findByCreatedById(Long userId, Pageable pageable);

    Page<SupportTicket> findByCreatedByIdAndStatus(Long userId, TicketStatus status, Pageable pageable);

    @Query("""
        SELECT t FROM SupportTicket t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:category IS NULL OR t.category = :category)
          AND (:slaBreached IS NULL OR t.slaBreached = :slaBreached)
          AND (:assignedToId IS NULL OR t.assignedTo.id = :assignedToId)
          AND (:search IS NULL
               OR LOWER(t.subject) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
               OR LOWER(t.ticketCode) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
    """)
    Page<SupportTicket> findAdminTickets(
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("category") TicketCategory category,
            @Param("slaBreached") Boolean slaBreached,
            @Param("assignedToId") Long assignedToId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM SupportTicket t
        WHERE t.status NOT IN ('RESOLVED', 'CLOSED')
          AND t.slaBreached = false
          AND t.slaDueAt < :now
    """)
    List<SupportTicket> findOverdueOpenTickets(@Param("now") OffsetDateTime now);

    long countByStatus(TicketStatus status);

    long countBySlaBreachedTrueAndStatusNotIn(List<TicketStatus> closedStatuses);
}
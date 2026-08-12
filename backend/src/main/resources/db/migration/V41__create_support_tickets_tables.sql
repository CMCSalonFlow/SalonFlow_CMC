-- Migration script for Support Ticket System
CREATE TABLE support_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_code VARCHAR(50) NOT NULL UNIQUE,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    priority VARCHAR(20) NOT NULL DEFAULT 'P3',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_to_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    sla_due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sla_breached BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE support_ticket_replies (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    is_internal_note BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_support_tickets_created_by ON support_tickets(created_by_user_id);
CREATE INDEX idx_support_tickets_assigned_to ON support_tickets(assigned_to_user_id);
CREATE INDEX idx_support_tickets_status ON support_tickets(status);
CREATE INDEX idx_support_tickets_priority ON support_tickets(priority);
CREATE INDEX idx_support_ticket_replies_ticket ON support_ticket_replies(ticket_id);

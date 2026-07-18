-- =====================================================
-- NOTIFICATIONS
-- =====================================================

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    booking_id BIGINT,
    channel VARCHAR(50) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(50) NOT NULL DEFAULT 'UNREAD',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    payload_json TEXT,
    source_type VARCHAR(100) NOT NULL,
    source_id BIGINT,
    event_type VARCHAR(100) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_notifications_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_recipient_created_at
    ON notifications(recipient_user_id, created_at DESC);

CREATE INDEX idx_notifications_status
    ON notifications(status);

CREATE INDEX idx_notifications_booking
    ON notifications(booking_id);

CREATE TRIGGER trg_notifications_updated_at
BEFORE UPDATE ON notifications
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

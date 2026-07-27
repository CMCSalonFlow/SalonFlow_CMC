-- US-037: Tao bang luu lich su gui SMS nhac hen (dedup)
CREATE TABLE IF NOT EXISTS sms_reminder_logs (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT       NOT NULL,
    reminder_type   VARCHAR(10)  NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    sent_at         TIMESTAMP    NOT NULL,
    success         BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Dedup: moi booking + loai nhac chi gui 1 lan
    CONSTRAINT uq_sms_reminder UNIQUE (booking_id, reminder_type),

    CONSTRAINT fk_sms_reminder_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE INDEX IF NOT EXISTS idx_sms_reminder_booking_type
    ON sms_reminder_logs(booking_id, reminder_type);

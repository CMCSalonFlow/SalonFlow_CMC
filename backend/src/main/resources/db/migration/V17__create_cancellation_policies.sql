-- Tạo bảng chính sách hủy lịch
CREATE TABLE IF NOT EXISTS cancellation_policies (
    id BIGSERIAL PRIMARY KEY,
    salon_id BIGINT NOT NULL,
    free_cancel_hours INTEGER DEFAULT 24,
    fee_percentage DECIMAL(5,2) DEFAULT 10.00,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cancellation_policy_salon
        FOREIGN KEY (salon_id)
        REFERENCES salons(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_salon_policy UNIQUE (salon_id)
);

-- Insert default policy cho các salon chưa có
INSERT INTO cancellation_policies (
    salon_id,
    free_cancel_hours,
    fee_percentage,
    is_active
)
SELECT
    id,
    24,
    10.00,
    true
FROM salons
WHERE id NOT IN (
    SELECT salon_id
    FROM cancellation_policies
)
ON CONFLICT DO NOTHING;
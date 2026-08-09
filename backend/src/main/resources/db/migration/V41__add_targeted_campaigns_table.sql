CREATE TABLE IF NOT EXISTS targeted_campaigns (
    id BIGSERIAL PRIMARY KEY,
    salon_id BIGINT NOT NULL,
    branch_id BIGINT NULL,
    campaign_name VARCHAR(255) NOT NULL,
    segment_type VARCHAR(50) NOT NULL,
    message_title VARCHAR(255) NOT NULL,
    message_content TEXT NOT NULL,
    voucher_id BIGINT NULL,
    recipient_count INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_targeted_campaigns_salon FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE,
    CONSTRAINT fk_targeted_campaigns_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE SET NULL,
    CONSTRAINT fk_targeted_campaigns_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers (id) ON DELETE SET NULL
);

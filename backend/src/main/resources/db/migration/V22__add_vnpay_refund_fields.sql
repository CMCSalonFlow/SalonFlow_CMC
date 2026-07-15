ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS gateway_transaction_date VARCHAR(32),
    ADD COLUMN IF NOT EXISTS refund_transaction_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS refund_amount NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMPTZ;

ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payments_status;
ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED'));

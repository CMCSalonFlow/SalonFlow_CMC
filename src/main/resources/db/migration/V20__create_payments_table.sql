-- =====================================================
-- BẢNG THANH TOÁN (PAYMENTS)
-- =====================================================
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    payment_method VARCHAR(50) NOT NULL, -- 'VNPAY', 'MOMO', 'ZALOPAY'
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SUCCESS', 'FAILED', 'CANCELLED'
    idempotency_key VARCHAR(255) NOT NULL UNIQUE, -- Tránh double charge
    gateway_transaction_id VARCHAR(255), -- Mã giao dịch từ đối tác (VNPay, MoMo, ZaloPay)
    payment_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,

    CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED')),

    CONSTRAINT chk_payments_method
        CHECK (payment_method IN ('VNPAY', 'MOMO', 'ZALOPAY'))
);

CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_idempotency_key ON payments(idempotency_key);

CREATE TRIGGER trg_payments_updated_at
BEFORE UPDATE ON payments
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

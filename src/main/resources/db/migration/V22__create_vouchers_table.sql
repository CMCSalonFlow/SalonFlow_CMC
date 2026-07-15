-- V22__create_vouchers_table.sql

CREATE TABLE vouchers (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)    NOT NULL UNIQUE,
    discount_type  VARCHAR(10) NOT NULL CHECK (discount_type IN ('FIXED', 'PERCENT')),
    discount_value NUMERIC(10, 2) NOT NULL CHECK (discount_value > 0),
    max_uses    INTEGER        NOT NULL DEFAULT 1,
    used_count  INTEGER        NOT NULL DEFAULT 0,
    expires_at  TIMESTAMP      NOT NULL,
    is_active   BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Bảng theo dõi user nào đã dùng voucher nào (đảm bảo 1 code/1 user)
CREATE TABLE voucher_usages (
    id          BIGSERIAL PRIMARY KEY,
    voucher_id  BIGINT      NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    booking_id  BIGINT      REFERENCES bookings(id) ON DELETE SET NULL,
    used_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (voucher_id, user_id)   -- 1 user chỉ dùng 1 lần
);

CREATE INDEX idx_vouchers_code      ON vouchers(code);
CREATE INDEX idx_vouchers_expires   ON vouchers(expires_at);
CREATE INDEX idx_voucher_usages_vid ON voucher_usages(voucher_id);
CREATE INDEX idx_voucher_usages_uid ON voucher_usages(user_id);

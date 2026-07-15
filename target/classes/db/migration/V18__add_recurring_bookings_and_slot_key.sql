-- =====================================================
-- RECURRING BOOKINGS (Idempotent)
-- =====================================================
-- Lưu thông tin "công thức" lặp lại (pattern) của đặt lịch.
CREATE TABLE IF NOT EXISTS recurring_bookings (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    service_id  BIGINT NOT NULL,
    staff_id    BIGINT NOT NULL, -- Liên kết với staff(id)
    branch_id   BIGINT NOT NULL,

    -- Pattern lặp: WEEKLY (mỗi tuần) hoặc BIWEEKLY (2 tuần/lần)
    pattern VARCHAR(20) NOT NULL,

    -- Ngày bắt đầu (booking đầu tiên)
    start_date DATE NOT NULL,

    -- Ngày kết thúc (không tạo booking sau ngày này)
    end_date DATE NOT NULL,

    -- Giờ bắt đầu/kết thúc — áp dụng cho TẤT CẢ các ngày lặp
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL,

    -- Trạng thái của cả chuỗi recurring
    -- ACTIVE: đang áp dụng | CANCELLED: đã hủy toàn bộ
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    note TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_recurring_bookings_customer
        FOREIGN KEY(customer_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_recurring_bookings_service
        FOREIGN KEY(service_id) REFERENCES services(id) ON DELETE RESTRICT,

    CONSTRAINT fk_recurring_bookings_staff
        FOREIGN KEY(staff_id) REFERENCES staff(id) ON DELETE RESTRICT,

    CONSTRAINT fk_recurring_bookings_branch
        FOREIGN KEY(branch_id) REFERENCES branches(id) ON DELETE RESTRICT,

    CONSTRAINT chk_recurring_bookings_pattern
        CHECK (pattern IN ('WEEKLY', 'BIWEEKLY')),

    CONSTRAINT chk_recurring_bookings_status
        CHECK (status IN ('ACTIVE', 'CANCELLED')),

    CONSTRAINT chk_recurring_bookings_dates
        CHECK (end_date >= start_date),

    CONSTRAINT chk_recurring_bookings_time
        CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_recurring_bookings_customer ON recurring_bookings(customer_id);
CREATE INDEX IF NOT EXISTS idx_recurring_bookings_staff    ON recurring_bookings(staff_id);
CREATE INDEX IF NOT EXISTS idx_recurring_bookings_status   ON recurring_bookings(status);

DROP TRIGGER IF EXISTS trg_recurring_bookings_updated_at ON recurring_bookings;
CREATE TRIGGER trg_recurring_bookings_updated_at
BEFORE UPDATE ON recurring_bookings
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- BỔ SUNG CỘT CHO BẢNG BOOKINGS
-- =====================================================
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS slot_key VARCHAR(255),
ADD COLUMN IF NOT EXISTS recurring_booking_id BIGINT;

ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_bookings_recurring;
ALTER TABLE bookings
ADD CONSTRAINT fk_bookings_recurring
    FOREIGN KEY(recurring_booking_id)
    REFERENCES recurring_bookings(id)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_bookings_recurring ON bookings(recurring_booking_id);
CREATE INDEX IF NOT EXISTS idx_bookings_slot_key ON bookings(slot_key);

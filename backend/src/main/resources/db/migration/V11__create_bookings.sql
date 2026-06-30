-- =====================================================
-- BOOKINGS
-- =====================================================
-- Lưu trữ thông tin đặt lịch của khách hàng.
-- Slot lock được xử lý bằng Redis (SETNX, TTL 600s)
-- trước khi booking được confirm vào bảng này.
--
-- Trạng thái booking:
--   PENDING   → đã lock slot, chờ xác nhận thanh toán
--   CONFIRMED → đã xác nhận
--   CANCELLED → đã hủy (slot được unlock)
--   COMPLETED → hoàn thành

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,

    -- Khách hàng đặt lịch
    customer_id BIGINT NOT NULL,

    -- Dịch vụ được đặt
    service_id BIGINT NOT NULL,

    -- Nhân viên thực hiện
    staff_id BIGINT NOT NULL,

    -- Chi nhánh
    branch_id BIGINT NOT NULL,

    -- Ngày và giờ bắt đầu
    booking_date DATE NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,

    -- Slot key dùng trong Redis để lock
    -- Format: "slot:{branch_id}:{staff_id}:{date}:{start_time}"
    slot_key VARCHAR(255) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    note TEXT,

    -- Giá tại thời điểm đặt (snapshot, tránh thay đổi giá ảnh hưởng booking cũ)
    price NUMERIC(12, 2) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_bookings_customer
        FOREIGN KEY(customer_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_bookings_service
        FOREIGN KEY(service_id)
        REFERENCES services(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bookings_staff
        FOREIGN KEY(staff_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bookings_branch
        FOREIGN KEY(branch_id)
        REFERENCES branches(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_bookings_status
        CHECK (status IN (
            'PENDING',
            'CONFIRMED',
            'CANCELLED',
            'COMPLETED'
        )),

    CONSTRAINT chk_bookings_time
        CHECK (end_time > start_time),

    -- Không cho phép 2 booking cùng staff, ngày, giờ bắt đầu
    CONSTRAINT uq_bookings_staff_date_time
        UNIQUE (staff_id, booking_date, start_time)
);

CREATE INDEX idx_bookings_customer    ON bookings(customer_id);
CREATE INDEX idx_bookings_staff_date  ON bookings(staff_id, booking_date);
CREATE INDEX idx_bookings_branch_date ON bookings(branch_id, booking_date);
CREATE INDEX idx_bookings_status      ON bookings(status);
CREATE INDEX idx_bookings_slot_key    ON bookings(slot_key);

CREATE TRIGGER trg_bookings_updated_at
BEFORE UPDATE ON bookings
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

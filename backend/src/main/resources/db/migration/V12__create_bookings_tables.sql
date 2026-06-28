-- =====================================================
-- BẢNG ĐẶT LỊCH HẸN (BOOKINGS)
-- =====================================================
-- Lưu thông tin đặt lịch của khách hàng tại chi nhánh
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    preferred_staff_id BIGINT, -- Nhân viên được yêu thích (nếu chọn)
    assigned_staff_id BIGINT,  -- Nhân viên được phân bổ thực tế
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_price NUMERIC(12, 2) NOT NULL,
    total_duration_minutes INT NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_bookings_customer
        FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_bookings_branch
        FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE,

    CONSTRAINT fk_bookings_preferred_staff
        FOREIGN KEY (preferred_staff_id) REFERENCES staff(id) ON DELETE SET NULL,

    CONSTRAINT fk_bookings_assigned_staff
        FOREIGN KEY (assigned_staff_id) REFERENCES staff(id) ON DELETE SET NULL,

    CONSTRAINT chk_bookings_price_positive
        CHECK (total_price >= 0),

    CONSTRAINT chk_bookings_duration_positive
        CHECK (total_duration_minutes > 0)
);

CREATE INDEX idx_bookings_customer ON bookings(customer_id);
CREATE INDEX idx_bookings_branch ON bookings(branch_id);
CREATE INDEX idx_bookings_assigned_staff ON bookings(assigned_staff_id);
CREATE INDEX idx_bookings_date ON bookings(booking_date);

CREATE TRIGGER trg_bookings_updated_at
BEFORE UPDATE ON bookings
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- BẢNG CHI TIẾT DỊCH VỤ ĐẶT LỊCH (BOOKING ITEMS)
-- =====================================================
-- Lưu thông tin chi tiết từng dịch vụ lẻ hoặc combo trong một lịch hẹn
CREATE TABLE booking_items (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    service_id BIGINT,
    bundle_id BIGINT,
    price NUMERIC(12, 2) NOT NULL,
    duration_minutes INT NOT NULL,

    CONSTRAINT fk_booking_items_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,

    CONSTRAINT fk_booking_items_service
        FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE SET NULL,

    CONSTRAINT fk_booking_items_bundle
        FOREIGN KEY (bundle_id) REFERENCES service_bundles(id) ON DELETE SET NULL,

    CONSTRAINT chk_booking_items_price_positive
        CHECK (price >= 0),

    CONSTRAINT chk_booking_items_duration_positive
        CHECK (duration_minutes > 0),

    -- Ràng buộc: Một bản ghi phải thuộc về dịch vụ lẻ HOẶC combo (không thể có cả hai hoặc không có cái nào)
    CONSTRAINT chk_item_source
        CHECK (
            (service_id IS NOT NULL AND bundle_id IS NULL) OR
            (service_id IS NULL AND bundle_id IS NOT NULL)
        )
);

CREATE INDEX idx_booking_items_booking ON booking_items(booking_id);

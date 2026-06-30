-- =====================================================
-- SERVICE BUNDLES
-- =====================================================
-- Bảng lưu thông tin các combo/gói dịch vụ
CREATE TABLE service_bundles (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL, -- Giá combo/gói ưu đãi
    original_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00, -- Tổng giá gốc của các dịch vụ trong combo (tự động tính)
    total_duration_minutes INT NOT NULL DEFAULT 0, -- Tổng thời gian của các dịch vụ trong combo (tự động tính)
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_service_bundles_branch
        FOREIGN KEY(branch_id)
        REFERENCES branches(id)
        ON DELETE CASCADE,

    -- Giá không được âm
    CONSTRAINT chk_service_bundles_price_positive
        CHECK (price >= 0),

    CONSTRAINT chk_service_bundles_original_price_positive
        CHECK (original_price >= 0),

    CONSTRAINT chk_service_bundles_duration_positive
        CHECK (total_duration_minutes >= 0)
);

CREATE INDEX idx_service_bundles_branch ON service_bundles(branch_id);
CREATE INDEX idx_service_bundles_is_active ON service_bundles(is_active);

CREATE TRIGGER trg_service_bundles_updated_at
BEFORE UPDATE ON service_bundles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SERVICE BUNDLE ITEMS
-- =====================================================
-- Bảng liên kết các dịch vụ trong combo
CREATE TABLE service_bundle_items (
    bundle_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (bundle_id, service_id),

    CONSTRAINT fk_service_bundle_items_bundle
        FOREIGN KEY(bundle_id)
        REFERENCES service_bundles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_bundle_items_service
        FOREIGN KEY(service_id)
        REFERENCES services(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_service_bundle_items_bundle ON service_bundle_items(bundle_id);
CREATE INDEX idx_service_bundle_items_service ON service_bundle_items(service_id);

-- =====================================================
-- TRIGGERS FOR AUTO STATS CALCULATION
-- =====================================================

-- 1. Trigger cập nhật tổng thời gian và giá gốc khi thay đổi service_bundle_items
CREATE OR REPLACE FUNCTION update_service_bundle_stats()
RETURNS TRIGGER AS $$
DECLARE
    v_bundle_id BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_bundle_id := OLD.bundle_id;
    ELSE
        v_bundle_id := NEW.bundle_id;
    END IF;

    UPDATE service_bundles
    SET 
        total_duration_minutes = COALESCE((
            SELECT SUM(s.duration_minutes)
            FROM service_bundle_items sbi
            JOIN services s ON sbi.service_id = s.id
            WHERE sbi.bundle_id = v_bundle_id
        ), 0),
        original_price = COALESCE((
            SELECT SUM(s.price)
            FROM service_bundle_items sbi
            JOIN services s ON sbi.service_id = s.id
            WHERE sbi.bundle_id = v_bundle_id
        ), 0.00),
        updated_at = NOW()
    WHERE id = v_bundle_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_service_bundle_items_changes
AFTER INSERT OR UPDATE OR DELETE ON service_bundle_items
FOR EACH ROW
EXECUTE FUNCTION update_service_bundle_stats();

-- 2. Trigger cập nhật tổng thời gian và giá gốc của bundle khi thông tin của service bị thay đổi
CREATE OR REPLACE FUNCTION update_service_bundle_stats_on_service_change()
RETURNS TRIGGER AS $$
BEGIN
    IF (OLD.price IS DISTINCT FROM NEW.price OR OLD.duration_minutes IS DISTINCT FROM NEW.duration_minutes) THEN
        UPDATE service_bundles sb
        SET 
            total_duration_minutes = COALESCE((
                SELECT SUM(s.duration_minutes)
                FROM service_bundle_items sbi
                JOIN services s ON sbi.service_id = s.id
                WHERE sbi.bundle_id = sb.id
            ), 0),
            original_price = COALESCE((
                SELECT SUM(s.price)
                FROM service_bundle_items sbi
                JOIN services s ON sbi.service_id = s.id
                WHERE sbi.bundle_id = sb.id
            ), 0.00),
            updated_at = NOW()
        WHERE sb.id IN (
            SELECT bundle_id 
            FROM service_bundle_items 
            WHERE service_id = NEW.id
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_services_stats_change
AFTER UPDATE ON services
FOR EACH ROW
EXECUTE FUNCTION update_service_bundle_stats_on_service_change();
-- =====================================================
-- SHIFTS
-- =====================================================
-- Shift thực tế đã được áp dụng vào 1 ngày cụ thể.
-- Sinh ra từ template khi dùng nút "Áp dụng template tuần này/tuần sau".
-- Tách riêng với template để:
--   1. Cho phép chỉnh sửa ca cụ thể mà không ảnh hưởng template
--   2. Dễ query availability slots theo ngày thực tế

CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    branch_id BIGINT NOT NULL,

    -- NULL nếu shift được tạo thủ công (không từ template)
    template_id BIGINT,

    shift_date DATE NOT NULL,

    start_time TIME NOT NULL,

    end_time TIME NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',

    note TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_shifts_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_shifts_branch
        FOREIGN KEY(branch_id)
        REFERENCES branches(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_shifts_template
        FOREIGN KEY(template_id)
        REFERENCES shift_templates(id)
        ON DELETE SET NULL,

    -- end_time phải sau start_time
    CONSTRAINT chk_shifts_time
        CHECK (end_time > start_time),

    -- status chỉ được nhận các giá trị hợp lệ
    CONSTRAINT chk_shifts_status
        CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),

    -- Mỗi staff không có 2 shift overlap trong cùng 1 ngày tại cùng 1 branch
    -- (validate overlap chi tiết hơn ở tầng service Java)
    CONSTRAINT uq_shifts_user_branch_date_time
        UNIQUE (user_id, branch_id, shift_date, start_time)
);

CREATE INDEX idx_shifts_user_date
ON shifts(user_id, shift_date);

CREATE INDEX idx_shifts_branch_date
ON shifts(branch_id, shift_date);

CREATE INDEX idx_shifts_template
ON shifts(template_id);

CREATE INDEX idx_shifts_status
ON shifts(status);

CREATE TRIGGER trg_shifts_updated_at
BEFORE UPDATE ON shifts
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

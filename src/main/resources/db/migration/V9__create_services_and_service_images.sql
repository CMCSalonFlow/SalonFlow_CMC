-- =====================================================
-- SERVICES
-- =====================================================
-- Dịch vụ cụ thể của 1 salon (cắt tóc, làm nail, spa...)
-- duration_minutes BẮT BUỘC là bội số của 15 vì ảnh hưởng
-- trực tiếp đến việc chia slot booking (xem CHECK constraint).

CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,

    branch_id BIGINT NOT NULL,

    category_id BIGINT,

    name VARCHAR(255) NOT NULL,

    price NUMERIC(12, 2) NOT NULL,

    duration_minutes INT NOT NULL,

    description TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_services_branch
        FOREIGN KEY(branch_id)
        REFERENCES branches(id)
        ON DELETE CASCADE,

    -- Category bị xóa thì service KHÔNG bị xóa theo,
    -- chỉ mất liên kết category (đặt NULL)
    CONSTRAINT fk_services_category
        FOREIGN KEY(category_id)
        REFERENCES service_categories(id)
        ON DELETE SET NULL,

    -- Giá không được âm
    CONSTRAINT chk_services_price_positive
        CHECK (price >= 0),

    -- ⚠️ AC quan trọng: duration ảnh hưởng trực tiếp đến slot booking
    -- nên bắt buộc là bội số của 15 phút và tối thiểu 15 phút
    CONSTRAINT chk_services_duration_multiple_of_15
        CHECK (
            duration_minutes > 0
            AND duration_minutes % 15 = 0
        )
);

CREATE INDEX idx_services_branch
ON services(branch_id);

CREATE INDEX idx_services_category
ON services(category_id);

CREATE INDEX idx_services_is_active
ON services(is_active);

CREATE TRIGGER trg_services_updated_at
BEFORE UPDATE ON services
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SERVICE IMAGES
-- =====================================================
-- 1 service có nhiều ảnh, theo cùng pattern với salon_photos (V6)

CREATE TABLE service_images (
    id BIGSERIAL PRIMARY KEY,

    service_id BIGINT NOT NULL,

    image_url VARCHAR(500) NOT NULL,

    display_order INT DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_service_images_service
        FOREIGN KEY(service_id)
        REFERENCES services(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_service_images_service
ON service_images(service_id);

-- =====================================================
-- SHIFT TEMPLATES
-- =====================================================
-- Template lịch làm việc tái sử dụng hàng tuần.
-- Mỗi template thuộc về 1 staff (user) trong 1 branch.
-- 1 staff có thể có nhiều template (ca sáng, ca tối...).

CREATE TABLE shift_templates (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    branch_id BIGINT NOT NULL,

    name VARCHAR(100) NOT NULL,

    description TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_shift_templates_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_shift_templates_branch
        FOREIGN KEY(branch_id)
        REFERENCES branches(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_shift_templates_user
ON shift_templates(user_id);

CREATE INDEX idx_shift_templates_branch
ON shift_templates(branch_id);

CREATE INDEX idx_shift_templates_user_branch
ON shift_templates(user_id, branch_id);

CREATE TRIGGER trg_shift_templates_updated_at
BEFORE UPDATE ON shift_templates
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SHIFT TEMPLATE DETAILS
-- =====================================================
-- Chi tiết từng ca trong template: ngày nào, mấy giờ.
-- day_of_week: 1=Thứ 2, 2=Thứ 3, ..., 6=Thứ 7, 7=Chủ nhật
-- (theo chuẩn ISO 8601, khác với Java DayOfWeek bắt đầu từ MONDAY=1)

CREATE TABLE shift_template_details (
    id BIGSERIAL PRIMARY KEY,

    template_id BIGINT NOT NULL,

    day_of_week INT NOT NULL,

    start_time TIME NOT NULL,

    end_time TIME NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_shift_template_details_template
        FOREIGN KEY(template_id)
        REFERENCES shift_templates(id)
        ON DELETE CASCADE,

    -- day_of_week phải từ 1 (Thứ 2) đến 7 (Chủ nhật)
    CONSTRAINT chk_shift_template_details_day
        CHECK (day_of_week BETWEEN 1 AND 7),

    -- end_time phải sau start_time
    CONSTRAINT chk_shift_template_details_time
        CHECK (end_time > start_time)
);

CREATE INDEX idx_shift_template_details_template
ON shift_template_details(template_id);

CREATE TRIGGER trg_shift_template_details_updated_at
BEFORE UPDATE ON shift_template_details
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

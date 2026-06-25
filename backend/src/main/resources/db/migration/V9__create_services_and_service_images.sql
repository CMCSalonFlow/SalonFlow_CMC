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

    created_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT fk_service_images_service
        FOREIGN KEY(service_id)
        REFERENCES services(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_service_images_service
ON service_images(service_id);
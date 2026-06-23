-- =====================================================
-- SERVICE CATEGORIES
-- =====================================================
-- Category dùng chung toàn hệ thống (không gắn theo salon)
-- Tương ứng entity: com.example.salonflow.entity.ServiceCategory

CREATE TABLE service_categories (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    icon VARCHAR(255),

    color VARCHAR(7) NOT NULL,

    description TEXT,

    display_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_service_categories_display_order
ON service_categories(display_order);

CREATE TRIGGER trg_service_categories_updated_at
BEFORE UPDATE ON service_categories
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
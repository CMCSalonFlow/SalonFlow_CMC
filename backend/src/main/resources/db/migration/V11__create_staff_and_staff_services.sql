-- =====================================================
-- BẢNG NHÂN VIÊN (STAFF)
-- =====================================================
-- Lưu thông tin chi tiết của nhân viên trong salon
CREATE TABLE staff (
    id BIGSERIAL PRIMARY KEY,
    salon_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    bio TEXT,
    specialties VARCHAR(500), -- Danh sách chuyên môn/tag kỹ năng (phân tách bằng dấu phẩy)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_staff_salon
        FOREIGN KEY (salon_id)
        REFERENCES salons(id)
        ON DELETE CASCADE
);

-- Chỉ mục để tối ưu hóa truy vấn nhân viên theo salon
CREATE INDEX idx_staff_salon ON staff(salon_id);

-- Trigger cập nhật thời gian thay đổi dữ liệu tự động
CREATE TRIGGER trg_staff_updated_at
BEFORE UPDATE ON staff
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- BẢNG LIÊN KẾT NHÂN VIÊN - DỊCH VỤ (STAFF SERVICES)
-- =====================================================
-- Mối quan hệ nhiều-nhiều (Many-to-Many) giữa Nhân viên và Dịch vụ được phép thực hiện
CREATE TABLE staff_services (
    staff_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (staff_id, service_id),

    CONSTRAINT fk_staff_services_staff
        FOREIGN KEY (staff_id)
        REFERENCES staff(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_staff_services_service
        FOREIGN KEY (service_id)
        REFERENCES services(id)
        ON DELETE CASCADE
);

-- Chỉ mục tối ưu hóa tìm kiếm quan hệ liên kết
CREATE INDEX idx_staff_services_staff ON staff_services(staff_id);
CREATE INDEX idx_staff_services_service ON staff_services(service_id);

-- V58: Thêm salon_id vào service_categories để phân quyền danh mục dịch vụ theo từng salon
-- Mỗi salon tự quản lý danh mục dịch vụ của riêng mình, tránh lộ hoặc sửa chéo danh mục

ALTER TABLE service_categories ADD COLUMN IF NOT EXISTS salon_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_service_categories_salon'
    ) THEN
        ALTER TABLE service_categories
            ADD CONSTRAINT fk_service_categories_salon
            FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_service_categories_salon_id ON service_categories(salon_id);

-- Cập nhật các danh mục hiện tại về cho salon Phong BVB nếu có trong hệ thống
UPDATE service_categories
SET salon_id = (SELECT id FROM salons WHERE LOWER(name) LIKE '%phong%bvb%' OR LOWER(name) LIKE '%bvb%' ORDER BY id ASC LIMIT 1)
WHERE salon_id IS NULL AND EXISTS (SELECT 1 FROM salons WHERE LOWER(name) LIKE '%phong%bvb%' OR LOWER(name) LIKE '%bvb%');

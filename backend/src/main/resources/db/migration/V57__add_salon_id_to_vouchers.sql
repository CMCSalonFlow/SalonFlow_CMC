-- V57: Thêm salon_id vào bảng vouchers để phân quyền voucher theo từng salon
-- Mỗi salon owner chỉ thấy và quản lý voucher của salon mình

ALTER TABLE vouchers ADD COLUMN IF NOT EXISTS salon_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_vouchers_salon'
    ) THEN
        ALTER TABLE vouchers
            ADD CONSTRAINT fk_vouchers_salon
            FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_vouchers_salon_id ON vouchers(salon_id);

-- Cập nhật các voucher hiện tại (chưa có salon_id) về cho salon Phong BVB nếu có trong hệ thống
UPDATE vouchers
SET salon_id = (SELECT id FROM salons WHERE LOWER(name) LIKE '%phong%bvb%' OR LOWER(name) LIKE '%bvb%' ORDER BY id ASC LIMIT 1)
WHERE salon_id IS NULL AND EXISTS (SELECT 1 FROM salons WHERE LOWER(name) LIKE '%phong%bvb%' OR LOWER(name) LIKE '%bvb%');

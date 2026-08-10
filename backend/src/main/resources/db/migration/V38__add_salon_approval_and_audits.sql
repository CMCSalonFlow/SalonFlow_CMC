-- Migration V36: Thêm các trường duyệt Salon và tạo bảng salon_approval_audits

-- 1. Thêm các cột cho bảng salons
ALTER TABLE salons ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'APPROVED';
ALTER TABLE salons ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE salons ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE salons ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITHOUT TIME ZONE;

-- Cập nhật dữ liệu cũ nếu status bị NULL thành APPROVED
UPDATE salons SET status = 'APPROVED' WHERE status IS NULL;

-- 2. Tạo bảng salon_approval_audits
CREATE TABLE IF NOT EXISTS salon_approval_audits (
    id BIGSERIAL PRIMARY KEY,
    salon_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_salon_approval_audits_salon FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE CASCADE,
    CONSTRAINT fk_salon_approval_audits_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_salon_approval_audits_salon_id ON salon_approval_audits(salon_id);

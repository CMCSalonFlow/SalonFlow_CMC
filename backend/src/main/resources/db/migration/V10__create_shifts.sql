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

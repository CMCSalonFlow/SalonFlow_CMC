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

    day_of_week SMALLINT NOT NULL,

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

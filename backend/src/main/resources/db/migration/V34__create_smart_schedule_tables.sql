-- =====================================================
-- SMART SCHEDULING TABLES: CONFIGS & LOGS
-- =====================================================

-- 1. Bảng cấu hình trọng số thuật toán Smart Schedule
CREATE TABLE IF NOT EXISTS smart_schedule_configs (
    id BIGSERIAL PRIMARY KEY,
    salon_id BIGINT NOT NULL UNIQUE,
    workload_weight NUMERIC(3, 2) NOT NULL DEFAULT 0.40,
    travel_gap_weight NUMERIC(3, 2) NOT NULL DEFAULT 0.30,
    service_fit_weight NUMERIC(3, 2) NOT NULL DEFAULT 0.30,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_smart_schedule_configs_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE CASCADE,

    CONSTRAINT chk_weights_valid
        CHECK (workload_weight >= 0 AND travel_gap_weight >= 0 AND service_fit_weight >= 0)
);

CREATE INDEX idx_smart_schedule_configs_salon ON smart_schedule_configs(salon_id);

CREATE TRIGGER trg_smart_schedule_configs_updated_at
BEFORE UPDATE ON smart_schedule_configs
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 2. Bảng lưu log các gợi ý slot AI phục vụ đánh giá hiệu quả
CREATE TABLE IF NOT EXISTS smart_schedule_logs (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    customer_id BIGINT,
    request_date DATE NOT NULL,
    service_ids VARCHAR(255),
    staff_id BIGINT,
    recommended_slots_json TEXT NOT NULL,
    selected_slot_time VARCHAR(20),
    is_booked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_smart_schedule_logs_branch
        FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE,

    CONSTRAINT fk_smart_schedule_logs_customer
        FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT fk_smart_schedule_logs_staff
        FOREIGN KEY (staff_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_smart_schedule_logs_branch_date ON smart_schedule_logs(branch_id, request_date);
CREATE INDEX idx_smart_schedule_logs_customer ON smart_schedule_logs(customer_id);

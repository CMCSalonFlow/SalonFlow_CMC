-- ============================================================
-- US-068: Audit Log - immutable, partition theo tháng
-- ============================================================

CREATE TABLE audit_logs (
    id              BIGSERIAL,
    user_id         BIGINT,
    user_email      VARCHAR(255),
    action          VARCHAR(50) NOT NULL,
    resource_type   VARCHAR(100) NOT NULL,
    resource_id     VARCHAR(100),
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_old_value_gin ON audit_logs USING GIN (old_value);
CREATE INDEX idx_audit_logs_new_value_gin ON audit_logs USING GIN (new_value);

-- ============================================================
-- Chặn UPDATE / DELETE trực tiếp (immutable) - lớp phòng thủ ở DB
-- ============================================================
CREATE OR REPLACE FUNCTION prevent_audit_log_modification()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is immutable: % is not allowed on this table', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_prevent_update
BEFORE UPDATE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_modification();

CREATE TRIGGER trg_audit_logs_prevent_delete
BEFORE DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_modification();

-- ============================================================
-- Hàm tự tạo partition theo tháng (gọi từ scheduler mỗi tháng)
-- ============================================================
CREATE OR REPLACE FUNCTION create_audit_log_partition(target_month DATE DEFAULT date_trunc('month', now())::date)
RETURNS void AS $$
DECLARE
    start_date DATE := date_trunc('month', target_month);
    end_date   DATE := start_date + interval '1 month';
    partition_name TEXT := 'audit_logs_' || to_char(start_date, 'YYYY_MM');
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = partition_name) THEN
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF audit_logs FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date
        );
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Hàm dọn partition quá 2 năm (GDPR retention) - gọi từ scheduler
-- ============================================================
CREATE OR REPLACE FUNCTION cleanup_old_audit_partitions()
RETURNS void AS $$
DECLARE
    r RECORD;
    cutoff DATE := date_trunc('month', now())::date - interval '24 months';
BEGIN
    FOR r IN
        SELECT relname FROM pg_class
        WHERE relname ~ '^audit_logs_\d{4}_\d{2}$'
        AND to_date(substring(relname from 'audit_logs_(\d{4}_\d{2})'), 'YYYY_MM') < cutoff
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I', r.relname);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Tạo partition cho tháng hiện tại + 2 tháng tới ngay khi migrate
SELECT create_audit_log_partition(date_trunc('month', now())::date);
SELECT create_audit_log_partition((date_trunc('month', now()) + interval '1 month')::date);
SELECT create_audit_log_partition((date_trunc('month', now()) + interval '2 month')::date);

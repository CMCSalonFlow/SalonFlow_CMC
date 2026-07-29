-- =====================================================
-- BỔ SUNG CỘT PHẢN HỒI, ĐỘ ẨN VÀ BẢNG BÁO CÁO VI PHẠM ĐÁNH GIÁ
-- =====================================================

ALTER TABLE reviews
ADD COLUMN IF NOT EXISTS owner_reply TEXT,
ADD COLUMN IF NOT EXISTS owner_replied_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS hidden_reason TEXT;

CREATE TABLE IF NOT EXISTS review_reports (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_notes TEXT,
    resolved_by BIGINT,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_review_reports_review
        FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,

    CONSTRAINT fk_review_reports_reporter
        FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_review_reports_resolver
        FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT chk_review_reports_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_review_reports_review ON review_reports(review_id);
CREATE INDEX IF NOT EXISTS idx_review_reports_reporter ON review_reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_review_reports_status ON review_reports(status);

DROP TRIGGER IF EXISTS trg_review_reports_updated_at ON review_reports;
CREATE TRIGGER trg_review_reports_updated_at
BEFORE UPDATE ON review_reports
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

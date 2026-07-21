-- =====================================================
-- REVIEWS
-- Bảng nền cho review và phân tích sentiment AI
-- =====================================================

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    booking_id BIGINT,
    branch_id BIGINT,
    staff_id BIGINT,
    rating INT,
    title VARCHAR(255),
    content TEXT NOT NULL,
    sentiment VARCHAR(20),
    sentiment_confidence NUMERIC(5,4),
    sentiment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sentiment_provider VARCHAR(50),
    sentiment_analyzed_at TIMESTAMPTZ,
    sentiment_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_reviews_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL,

    CONSTRAINT fk_reviews_branch
        FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE SET NULL,

    CONSTRAINT fk_reviews_staff
        FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE SET NULL,

    CONSTRAINT chk_reviews_rating
        CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),

    CONSTRAINT chk_reviews_sentiment
        CHECK (sentiment IS NULL OR sentiment IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),

    CONSTRAINT chk_reviews_sentiment_status
        CHECK (sentiment_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_reviews_user_created_at
    ON reviews(user_id, created_at DESC);

CREATE INDEX idx_reviews_booking
    ON reviews(booking_id);

CREATE INDEX idx_reviews_branch_created_at
    ON reviews(branch_id, created_at DESC);

CREATE INDEX idx_reviews_staff_created_at
    ON reviews(staff_id, created_at DESC);

CREATE INDEX idx_reviews_sentiment_status
    ON reviews(sentiment_status);

CREATE INDEX idx_reviews_sentiment
    ON reviews(sentiment);

CREATE TRIGGER trg_reviews_updated_at
BEFORE UPDATE ON reviews
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


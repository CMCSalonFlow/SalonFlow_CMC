-- =====================================================
-- BẢNG REVIEW VÀ REVIEW_PHOTOS
-- =====================================================

-- 1. Bổ sung cột reviewed_at vào bảng bookings
ALTER TABLE bookings
ADD COLUMN reviewed_at TIMESTAMPTZ NULL;

-- 2. Bổ sung cột rating_average và rating_count vào bảng salons
ALTER TABLE salons
ADD COLUMN rating_average NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
ADD COLUMN rating_count INT NOT NULL DEFAULT 0;

-- 3. Tạo bảng reviews
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    salon_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_reviews_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,

    CONSTRAINT fk_reviews_customer
        FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_reviews_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE CASCADE,

    CONSTRAINT fk_reviews_branch
        FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);

CREATE INDEX idx_reviews_booking ON reviews(booking_id);
CREATE INDEX idx_reviews_customer ON reviews(customer_id);
CREATE INDEX idx_reviews_salon ON reviews(salon_id);
CREATE INDEX idx_reviews_branch ON reviews(branch_id);

CREATE TRIGGER trg_reviews_updated_at
BEFORE UPDATE ON reviews
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 4. Tạo bảng review_photos (Tối đa 5 ảnh / review)
CREATE TABLE review_photos (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    media_id BIGINT,
    photo_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_review_photos_review
        FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,

    CONSTRAINT fk_review_photos_media
        FOREIGN KEY (media_id) REFERENCES media_files(id) ON DELETE SET NULL
);

CREATE INDEX idx_review_photos_review ON review_photos(review_id);

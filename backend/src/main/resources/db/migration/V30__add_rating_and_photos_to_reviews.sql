-- =====================================================
-- BỔ SUNG CỘT CHO BẢNG REVIEWS VÀ TẠO BẢNG REVIEW_PHOTOS
-- =====================================================

-- 1. Bổ sung cột reviewed_at vào bảng bookings
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ NULL;

-- 2. Bổ sung cột rating_average và rating_count vào bảng salons và branches
ALTER TABLE salons
ADD COLUMN IF NOT EXISTS rating_average NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS rating_count INT NOT NULL DEFAULT 0;

ALTER TABLE branches
ADD COLUMN IF NOT EXISTS rating_average NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS rating_count INT NOT NULL DEFAULT 0;

-- 3. Bổ sung cột salon_id vào bảng reviews (đã được tạo từ V28)
ALTER TABLE reviews
ADD COLUMN IF NOT EXISTS salon_id BIGINT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_reviews_salon'
    ) THEN
        ALTER TABLE reviews
        ADD CONSTRAINT fk_reviews_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_reviews_salon ON reviews(salon_id);

-- 4. Tạo bảng review_photos (Tối đa 5 ảnh / review)
CREATE TABLE IF NOT EXISTS review_photos (
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

CREATE INDEX IF NOT EXISTS idx_review_photos_review ON review_photos(review_id);

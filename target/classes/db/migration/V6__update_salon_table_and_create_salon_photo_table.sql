-- =====================================================
-- 1. SALON HOURS TABLE
-- =====================================================

CREATE TABLE salon_hours (
    id BIGSERIAL PRIMARY KEY,

    salon_id BIGINT NOT NULL,

    day_of_week INT NOT NULL,
    -- 0 = Sunday ... 6 = Saturday

    open_time TIME,
    close_time TIME,

    is_closed BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_salon_hours_salon
        FOREIGN KEY (salon_id)
        REFERENCES salons(id)
        ON DELETE CASCADE,

    CONSTRAINT check_day_of_week
        CHECK (day_of_week BETWEEN 0 AND 6)
);

-- mỗi salon chỉ có 1 record cho mỗi ngày
ALTER TABLE salon_hours
ADD CONSTRAINT unique_salon_day
UNIQUE (salon_id, day_of_week);


-- =====================================================
-- 2. SALON PHOTOS TABLE
-- =====================================================

CREATE TABLE salon_photos (
    id BIGSERIAL PRIMARY KEY,

    salon_id BIGINT NOT NULL,

    media_id BIGINT,

    is_primary BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_salon_photos_salon
        FOREIGN KEY (salon_id)
        REFERENCES salons(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_salon_photos_salon ON salon_photos(salon_id);
CREATE INDEX idx_salon_photos_media ON salon_photos(media_id);

CREATE TRIGGER trg_salon_photos_updated_at
BEFORE UPDATE ON salon_photos
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


-- =====================================================
-- 3. UNIQUE OWNER CHECK
-- =====================================================

ALTER TABLE salons
ADD CONSTRAINT unique_owner_id UNIQUE (owner_id);


-- =====================================================
-- 4. FK TO MEDIA FILES WILL BE ADDED IN V8
-- =====================================================

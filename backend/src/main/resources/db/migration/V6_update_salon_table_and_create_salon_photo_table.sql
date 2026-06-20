-- =====================================================
-- 1. ADD ADDRESS + GEO LOCATION TO SALONS (FROM V5)
-- =====================================================

ALTER TABLE salons
ADD COLUMN address TEXT;

ALTER TABLE salons
ADD COLUMN latitude DOUBLE PRECISION;

ALTER TABLE salons
ADD COLUMN longitude DOUBLE PRECISION;


-- =====================================================
-- 2. INDEX FOR LOCATION SEARCH
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_salons_location
ON salons(latitude, longitude);


-- =====================================================
-- 3. SALON HOURS TABLE (NEW)
-- =====================================================

CREATE TABLE salon_hours (
    id BIGSERIAL PRIMARY KEY,

    salon_id BIGINT NOT NULL,

    day_of_week SMALLINT NOT NULL,
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
-- 4. SALON PHOTOS TABLE (NEW)
-- =====================================================

CREATE TABLE salon_photos (
    id BIGSERIAL PRIMARY KEY,

    salon_id BIGINT NOT NULL,

    url TEXT NOT NULL,

    is_primary BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_salon_photos_salon
        FOREIGN KEY (salon_id)
        REFERENCES salons(id)
        ON DELETE CASCADE
);


-- =====================================================
-- 5. (OPTIONAL SAFETY) UNIQUE OWNER CHECK (IF NOT EXISTS BEFORE)
-- =====================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'unique_owner_id'
    ) THEN
        ALTER TABLE salons
        ADD CONSTRAINT unique_owner_id UNIQUE (owner_id);
    END IF;
END $$;


-- =====================================================
-- 6. (OPTIONAL) MAKE SURE TRIGGER EXISTS ONLY ONCE
-- =====================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'trg_salons_updated_at'
    ) THEN
        CREATE TRIGGER trg_salons_updated_at
        BEFORE UPDATE ON salons
        FOR EACH ROW
        EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;
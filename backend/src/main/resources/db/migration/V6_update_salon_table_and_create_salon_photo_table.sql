-- ===========================
-- Update salons table
-- ===========================

ALTER TABLE salons
ADD COLUMN address TEXT,

ADD COLUMN latitude DOUBLE PRECISION,

ADD COLUMN longitude DOUBLE PRECISION,

ADD COLUMN opening_hours JSONB DEFAULT '{}'::jsonb;

-- Index phục vụ tìm kiếm theo tọa độ
CREATE INDEX idx_salons_location
ON salons(latitude, longitude);

CREATE TABLE salon_photos (

    id BIGSERIAL PRIMARY KEY,

    salon_id BIGINT NOT NULL,

    image_url VARCHAR(500) NOT NULL,

    display_order INT DEFAULT 0,

    created_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT fk_salon_photo
        FOREIGN KEY(salon_id)
        REFERENCES salons(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_salon_photos
ON salon_photos(salon_id);
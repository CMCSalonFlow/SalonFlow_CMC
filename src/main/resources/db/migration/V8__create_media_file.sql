CREATE TABLE media_files (
    id BIGSERIAL PRIMARY KEY,

    object_name VARCHAR(255),

    original_file_name VARCHAR(255),

    content_type VARCHAR(255),

    file_size BIGINT,

    url VARCHAR(500),

    provider VARCHAR(50),

    bucket VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_files_provider ON media_files(provider);

CREATE TRIGGER trg_media_files_updated_at
BEFORE UPDATE ON media_files
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE salons
ADD CONSTRAINT fk_salons_logo_media
FOREIGN KEY (logo_media_id)
REFERENCES media_files(id)
ON DELETE SET NULL;

ALTER TABLE salon_photos
ADD CONSTRAINT fk_salon_photos_media
FOREIGN KEY (media_id)
REFERENCES media_files(id)
ON DELETE SET NULL;

ALTER TABLE service_categories
ADD CONSTRAINT fk_service_categories_icon_media
FOREIGN KEY (icon_media_id)
REFERENCES media_files(id)
ON DELETE SET NULL;

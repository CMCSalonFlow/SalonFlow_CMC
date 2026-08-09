-- =====================================================
-- HAIR STYLE RECOMMENDATION DOMAIN
-- =====================================================

CREATE TABLE hair_styles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    face_shape_tags TEXT,
    hair_texture_tags TEXT,
    hair_length_tags TEXT,
    hair_density_tags TEXT,
    difficulty_level VARCHAR(20),
    maintenance_level VARCHAR(20),
    price_min NUMERIC(12, 2),
    price_max NUMERIC(12, 2),
    popularity_score NUMERIC(6, 4) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hair_styles_active_sort
    ON hair_styles(is_active, sort_order, popularity_score DESC, name);

CREATE TRIGGER trg_hair_styles_updated_at
BEFORE UPDATE ON hair_styles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE hair_style_images (
    id BIGSERIAL PRIMARY KEY,
    hair_style_id BIGINT NOT NULL,
    media_id BIGINT,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    image_quality_score NUMERIC(6, 4) NOT NULL DEFAULT 0,
    ai_aesthetic_score NUMERIC(6, 4) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_hair_style_images_hair_style
        FOREIGN KEY (hair_style_id)
        REFERENCES hair_styles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_hair_style_images_media
        FOREIGN KEY (media_id)
        REFERENCES media_files(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_hair_style_images_style_active
    ON hair_style_images(hair_style_id, is_active, is_cover, display_order);
CREATE INDEX idx_hair_style_images_media
    ON hair_style_images(media_id);

CREATE TRIGGER trg_hair_style_images_updated_at
BEFORE UPDATE ON hair_style_images
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE hair_analysis_results (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    media_id BIGINT,
    provider VARCHAR(50),
    analysis_version VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    face_shape VARCHAR(30),
    hair_texture VARCHAR(30),
    hair_length VARCHAR(30),
    hair_density VARCHAR(30),
    current_style VARCHAR(255),
    confidence NUMERIC(6, 4),
    raw_response TEXT,
    error_message TEXT,
    analyzed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_hair_analysis_results_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_hair_analysis_results_media
        FOREIGN KEY (media_id)
        REFERENCES media_files(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_hair_analysis_results_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),

    CONSTRAINT chk_hair_analysis_results_confidence
        CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

CREATE INDEX idx_hair_analysis_results_user_created
    ON hair_analysis_results(user_id, created_at DESC);
CREATE INDEX idx_hair_analysis_results_status_created
    ON hair_analysis_results(status, created_at ASC);

CREATE TRIGGER trg_hair_analysis_results_updated_at
BEFORE UPDATE ON hair_analysis_results
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE customer_hair_profiles (
    user_id BIGINT PRIMARY KEY,
    selected_hair_style_id BIGINT,
    selected_hair_style_image_id BIGINT,
    latest_analysis_result_id BIGINT,
    face_shape VARCHAR(30),
    hair_texture VARCHAR(30),
    hair_length VARCHAR(30),
    hair_density VARCHAR(30),
    current_style VARCHAR(255),
    profile_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_customer_hair_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_customer_hair_profiles_style
        FOREIGN KEY (selected_hair_style_id)
        REFERENCES hair_styles(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_customer_hair_profiles_image
        FOREIGN KEY (selected_hair_style_image_id)
        REFERENCES hair_style_images(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_customer_hair_profiles_analysis
        FOREIGN KEY (latest_analysis_result_id)
        REFERENCES hair_analysis_results(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_customer_hair_profiles_style
    ON customer_hair_profiles(selected_hair_style_id);

CREATE TRIGGER trg_customer_hair_profiles_updated_at
BEFORE UPDATE ON customer_hair_profiles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

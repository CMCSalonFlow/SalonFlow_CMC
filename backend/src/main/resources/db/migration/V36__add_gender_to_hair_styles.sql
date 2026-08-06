ALTER TABLE hair_styles
    ADD COLUMN gender VARCHAR(20) NOT NULL DEFAULT 'UNISEX';

CREATE INDEX idx_hair_styles_gender_active_sort
    ON hair_styles(gender, is_active, sort_order, popularity_score DESC, name);

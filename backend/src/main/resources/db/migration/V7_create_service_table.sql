CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,

    salon_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,

    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    duration_minutes INTEGER NOT NULL,

    description TEXT,

    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),

    CONSTRAINT fk_services_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE CASCADE,

    CONSTRAINT fk_services_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE TABLE service_bundles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    total_duration INTEGER NOT NULL DEFAULT 0, -- tổng phút (tự tính)
    original_price NUMERIC(12,2) NOT NULL DEFAULT 0, -- tổng giá gốc
    combo_price NUMERIC(12,2) NOT NULL, -- giá bán combo

    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE service_bundle_items (
    id BIGSERIAL PRIMARY KEY,
    bundle_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,

    quantity INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT fk_bundle FOREIGN KEY (bundle_id) REFERENCES service_bundles(id) ON DELETE CASCADE,
    CONSTRAINT fk_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
);

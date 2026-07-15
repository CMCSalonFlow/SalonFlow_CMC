-- =====================================================
-- CUSTOMER PROFILES
-- =====================================================

CREATE TABLE customer_profiles (
    user_id BIGINT PRIMARY KEY,

    membership_code VARCHAR(255) UNIQUE,
    loyalty_points INT,
    birthday DATE,
    gender VARCHAR(50),
    address TEXT,
    note TEXT,
    salon_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_customer_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_customer_profiles_salon
        FOREIGN KEY (salon_id)
        REFERENCES salons(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_customer_profiles_salon
ON customer_profiles(salon_id);

CREATE TRIGGER trg_customer_profiles_updated_at
BEFORE UPDATE ON customer_profiles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- REFRESH TOKENS
-- =====================================================

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user
ON refresh_tokens(user_id);

CREATE TRIGGER trg_refresh_tokens_updated_at
BEFORE UPDATE ON refresh_tokens
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

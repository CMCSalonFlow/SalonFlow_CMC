-- =====================================================
-- USER FCM TOKENS
-- =====================================================

CREATE TABLE user_fcm_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(4096) NOT NULL UNIQUE,
    device_name VARCHAR(255),
    platform VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user_fcm_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_fcm_tokens_user_active
    ON user_fcm_tokens(user_id, is_active);

CREATE TRIGGER trg_user_fcm_tokens_updated_at
BEFORE UPDATE ON user_fcm_tokens
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

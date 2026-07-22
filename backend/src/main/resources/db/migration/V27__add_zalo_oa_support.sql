-- =====================================================
-- ZALO OA & ZNS SUPPORT MIGRATION
-- =====================================================

-- 1. Add zalo_user_id column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS zalo_user_id VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_users_zalo_user_id ON users(zalo_user_id);

-- 2. Create zalo_tokens table to persist access and refresh tokens
CREATE TABLE IF NOT EXISTS zalo_tokens (
    id BIGSERIAL PRIMARY KEY,
    oa_id VARCHAR(100) UNIQUE NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_zalo_tokens_updated_at
BEFORE UPDATE ON zalo_tokens
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

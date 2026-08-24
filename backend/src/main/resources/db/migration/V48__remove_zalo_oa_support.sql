-- =====================================================
-- REMOVE ZALO OA & ZNS SUPPORT MIGRATION
-- =====================================================

DROP TRIGGER IF EXISTS trg_zalo_tokens_updated_at ON zalo_tokens;
DROP TABLE IF EXISTS zalo_tokens CASCADE;

DROP INDEX IF EXISTS idx_users_zalo_user_id;
ALTER TABLE users DROP COLUMN IF EXISTS zalo_user_id;

-- =====================================================
-- ADD PER-BRANCH SMS CONFIGURATION FIELDS
-- =====================================================

ALTER TABLE branches ADD COLUMN IF NOT EXISTS is_sms_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE branches ADD COLUMN IF NOT EXISTS sms_template VARCHAR(255) DEFAULT NULL;

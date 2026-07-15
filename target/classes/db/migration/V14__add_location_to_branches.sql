-- =====================================================
-- ADD GEO LOCATION TO BRANCHES
-- =====================================================

ALTER TABLE branches
ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;

ALTER TABLE branches
ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

CREATE INDEX IF NOT EXISTS idx_branches_location
ON branches(latitude, longitude);

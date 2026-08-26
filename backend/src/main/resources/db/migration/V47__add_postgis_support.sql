-- =====================================================
-- ENABLE POSTGIS EXTENSION & SPATIAL INDEXING FOR BRANCHES
-- =====================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- Create GiST index on geography point of branches for fast ST_DWithin and ST_Distance queries
CREATE INDEX IF NOT EXISTS idx_branches_geography
ON branches USING GIST (
    CAST(ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) AS geography)
);

-- Backfill sample coordinates for any existing branches that do not have coordinates
-- (Centering around TP. Hồ Chí Minh & Hà Nội key locations)
UPDATE branches
SET latitude = 10.776889, longitude = 106.700806
WHERE (latitude IS NULL OR longitude IS NULL) AND id % 4 = 0;

UPDATE branches
SET latitude = 10.782889, longitude = 106.698806
WHERE (latitude IS NULL OR longitude IS NULL) AND id % 4 = 1;

UPDATE branches
SET latitude = 10.762889, longitude = 106.682806
WHERE (latitude IS NULL OR longitude IS NULL) AND id % 4 = 2;

UPDATE branches
SET latitude = 10.792889, longitude = 106.692806
WHERE (latitude IS NULL OR longitude IS NULL);

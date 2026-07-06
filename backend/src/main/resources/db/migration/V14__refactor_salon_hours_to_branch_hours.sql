-- Drop constraints & foreign keys
ALTER TABLE salon_hours DROP CONSTRAINT IF EXISTS fk_salon_hours_salon;
ALTER TABLE salon_hours DROP CONSTRAINT IF EXISTS unique_salon_day;

-- Rename table
ALTER TABLE salon_hours RENAME TO branch_hours;

-- Rename column
ALTER TABLE branch_hours RENAME COLUMN salon_id TO branch_id;

-- Clear old data (safety)
TRUNCATE TABLE branch_hours;

-- Add new foreign key constraint to branches
ALTER TABLE branch_hours
ADD CONSTRAINT fk_branch_hours_branch
FOREIGN KEY (branch_id)
REFERENCES branches(id)
ON DELETE CASCADE;

-- Add unique constraint per branch per day
ALTER TABLE branch_hours
ADD CONSTRAINT unique_branch_day
UNIQUE (branch_id, day_of_week);

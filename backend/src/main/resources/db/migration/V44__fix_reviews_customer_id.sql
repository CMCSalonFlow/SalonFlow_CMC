-- Fix reviews customer_id / user_id column alignment
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE reviews ALTER COLUMN user_id DROP NOT NULL;

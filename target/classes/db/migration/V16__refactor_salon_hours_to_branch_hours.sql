DO $$
DECLARE
    r RECORD;
BEGIN
    -- If salon_hours exists, rename it to branch_hours
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'salon_hours') THEN
        -- Drop constraints if they exist
        ALTER TABLE salon_hours DROP CONSTRAINT IF EXISTS fk_salon_hours_salon;
        ALTER TABLE salon_hours DROP CONSTRAINT IF EXISTS unique_salon_day;

        -- Drop branch_hours if it already exists (leftover from incomplete db reset)
        DROP TABLE IF EXISTS branch_hours CASCADE;

        -- Rename table
        ALTER TABLE salon_hours RENAME TO branch_hours;

        -- Rename column
        ALTER TABLE branch_hours RENAME COLUMN salon_id TO branch_id;
    END IF;

    -- Dynamically drop any foreign keys pointing to salons (like Hibernate-generated ones)
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'branch_hours') THEN
        FOR r IN 
            SELECT tc.constraint_name 
            FROM information_schema.table_constraints tc 
            JOIN information_schema.key_column_usage kcu 
              ON tc.constraint_name = kcu.constraint_name 
            JOIN information_schema.constraint_column_usage ccu 
              ON ccu.constraint_name = tc.constraint_name 
            WHERE tc.constraint_type = 'FOREIGN KEY' 
              AND tc.table_name = 'branch_hours' 
              AND ccu.table_name = 'salons'
        LOOP
            EXECUTE 'ALTER TABLE branch_hours DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
        END LOOP;
    END IF;

    -- If branch_hours does not exist at this point, create it
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'branch_hours') THEN
        CREATE TABLE branch_hours (
            id BIGSERIAL PRIMARY KEY,
            branch_id BIGINT NOT NULL,
            day_of_week INT NOT NULL,
            open_time TIME,
            close_time TIME,
            is_closed BOOLEAN DEFAULT FALSE,
            CONSTRAINT check_day_of_week CHECK (day_of_week BETWEEN 0 AND 6)
        );
    END IF;

    -- Clear old data (safety)
    TRUNCATE TABLE branch_hours;

    -- Add new foreign key constraint to branches if it doesn't exist
    IF NOT EXISTS (SELECT FROM information_schema.table_constraints WHERE constraint_name = 'fk_branch_hours_branch') THEN
        ALTER TABLE branch_hours
        ADD CONSTRAINT fk_branch_hours_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
        ON DELETE CASCADE;
    END IF;

    -- Add unique constraint per branch per day if it doesn't exist
    IF NOT EXISTS (SELECT FROM information_schema.table_constraints WHERE constraint_name = 'unique_branch_day') THEN
        ALTER TABLE branch_hours
        ADD CONSTRAINT unique_branch_day
        UNIQUE (branch_id, day_of_week);
    END IF;
END $$;

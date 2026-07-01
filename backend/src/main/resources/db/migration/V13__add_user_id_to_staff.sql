-- Add user_id to staff table
ALTER TABLE staff ADD COLUMN user_id BIGINT;

-- Add foreign key constraint linking staff to user_branches via composite key
ALTER TABLE staff ADD CONSTRAINT fk_staff_user_branch
    FOREIGN KEY (user_id, branch_id)
    REFERENCES user_branches(user_id, branch_id)
    ON DELETE SET NULL;

-- A user can only be associated with one staff profile
ALTER TABLE staff ADD CONSTRAINT uq_staff_user UNIQUE (user_id);

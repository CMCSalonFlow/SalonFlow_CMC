-- =====================================================
-- SALONS
-- =====================================================

CREATE TABLE salons (
    id BIGSERIAL PRIMARY KEY,

    owner_id BIGINT NOT NULL,

    name VARCHAR(255) NOT NULL,

    description TEXT,

    logo_url VARCHAR(500),

    phone VARCHAR(20),

    email VARCHAR(255),

    website VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_salons_owner
        FOREIGN KEY(owner_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_salons_owner
ON salons(owner_id);

CREATE TRIGGER trg_salons_updated_at
BEFORE UPDATE ON salons
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- BRANCHES
-- =====================================================

CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,

    salon_id BIGINT NOT NULL,

    name VARCHAR(255) NOT NULL,

    phone VARCHAR(20),

    email VARCHAR(255),

    address TEXT NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_branch_salon
        FOREIGN KEY(salon_id)
        REFERENCES salons(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_branches_salon
ON branches(salon_id);

CREATE TRIGGER trg_branches_updated_at
BEFORE UPDATE ON branches
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- USER_BRANCHES
-- =====================================================

CREATE TABLE user_branches (

    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY(user_id, branch_id),

    CONSTRAINT fk_user_branches_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_branches_branch
        FOREIGN KEY(branch_id)
        REFERENCES branches(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_branches_user
ON user_branches(user_id);

CREATE INDEX idx_user_branches_branch
ON user_branches(branch_id);
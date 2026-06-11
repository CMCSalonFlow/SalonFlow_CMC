-- =====================================================
-- EXTENSIONS
-- =====================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- ENUMS
-- =====================================================

CREATE TYPE user_status AS ENUM (
'ACTIVE',
'INACTIVE',
'LOCKED'
);

-- =====================================================
-- USERS
-- =====================================================

CREATE TABLE users (
id BIGSERIAL PRIMARY KEY,
username VARCHAR(100) UNIQUE,
email VARCHAR(255) NOT NULL UNIQUE,
password_hash VARCHAR(255) NOT NULL,

full_name VARCHAR(255),
phone VARCHAR(20) UNIQUE,

avatar_url VARCHAR(500),

status user_status NOT NULL DEFAULT 'ACTIVE',

created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- =====================================================
-- ROLES
-- =====================================================

CREATE TABLE roles (
id BIGSERIAL PRIMARY KEY,

name VARCHAR(100) NOT NULL UNIQUE,
description TEXT,

created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()


);

CREATE INDEX idx_roles_name ON roles(name);

-- =====================================================
-- PERMISSIONS
-- =====================================================

CREATE TABLE permissions (
id BIGSERIAL PRIMARY KEY,
code VARCHAR(100) NOT NULL UNIQUE,
description TEXT,

created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

);

CREATE INDEX idx_permissions_code ON permissions(code);

-- =====================================================
-- USER ROLES
-- =====================================================

CREATE TABLE user_roles (
user_id BIGINT NOT NULL,
role_id BIGINT NOT NULL,

assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

PRIMARY KEY(user_id, role_id),

CONSTRAINT fk_user_roles_user
    FOREIGN KEY(user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

CONSTRAINT fk_user_roles_role
    FOREIGN KEY(role_id)
    REFERENCES roles(id)
    ON DELETE CASCADE

);

-- =====================================================
-- ROLE PERMISSIONS
-- =====================================================

CREATE TABLE role_permissions (
role_id BIGINT NOT NULL,
permission_id BIGINT NOT NULL,

created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

PRIMARY KEY(role_id, permission_id),

CONSTRAINT fk_role_permissions_role
    FOREIGN KEY(role_id)
    REFERENCES roles(id)
    ON DELETE CASCADE,

CONSTRAINT fk_role_permissions_permission
    FOREIGN KEY(permission_id)
    REFERENCES permissions(id)
    ON DELETE CASCADE

);

-- =====================================================
-- UPDATE TRIGGER
-- =====================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS
$$
BEGIN
NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_roles_updated_at
BEFORE UPDATE ON roles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_permissions_updated_at
BEFORE UPDATE ON permissions
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

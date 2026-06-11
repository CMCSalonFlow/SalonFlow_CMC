INSERT INTO roles(name, description)
VALUES
('SUPER_ADMIN', 'System Administrator'),
('SALON_OWNER', 'Salon Owner'),
('STAFF', 'Salon Staff'),
('CUSTOMER', 'Customer');

INSERT INTO permissions(code, description)
VALUES

-- USER
('user:create', 'Create user'),
('user:view', 'View user'),
('user:update', 'Update user'),
('user:delete', 'Delete user'),

-- SALON
('salon:create', 'Create salon'),
('salon:view', 'View salon'),
('salon:update', 'Update salon');

INSERT INTO role_permissions(role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'SUPER_ADMIN'),
    id
FROM permissions;

INSERT INTO role_permissions(role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name='SALON_OWNER'),
    id
FROM permissions
WHERE code IN (

'salon:create',
'salon:view',
'salon:update'
);

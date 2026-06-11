INSERT INTO roles(name, description)
VALUES
('SUPER_ADMIN', 'System Administrator'),
('SALON_OWNER', 'Salon Owner'),
('STAFF', 'Salon Staff'),
('CUSTOMER', 'Customer');

INSERT INTO permissions(code, description)
VALUES

('user:create', 'Create user'),
('user:view', 'View user'),
('user:update', 'Update user'),
('user:delete', 'Delete user'),

('salon:create', 'Create salon'),
('salon:view', 'View salon'),
('salon:update', 'Update salon'),
('salon:delete', 'Delete salon'),

('service:create', 'Create service'),
('service:view', 'View service'),
('service:update', 'Update service'),
('service:delete', 'Delete service'),

('booking:create', 'Create booking'),
('booking:view', 'View booking'),
('booking:update', 'Update booking'),
('booking:cancel', 'Cancel booking'),

('staff:create', 'Create staff'),
('staff:view', 'View staff'),
('staff:update', 'Update staff'),
('staff:delete', 'Delete staff'),

('profile:view', 'View profile'),
('profile:update', 'Update profile');

INSERT INTO role_permissions(role_id, permission_id)
SELECT
(SELECT id FROM roles WHERE name='SUPER_ADMIN'),
id
FROM permissions
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT
(SELECT id FROM roles WHERE name='SALON_OWNER'),
id
FROM permissions
WHERE code IN (
'salon:create',
'salon:view',
'salon:update',
'service:create',
'service:view',
'service:update',
'staff:create',
'staff:view',
'staff:update',
'booking:view'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT
(SELECT id FROM roles WHERE name='CUSTOMER'),
id
FROM permissions
WHERE code IN (
'profile:view',
'profile:update',
'booking:create',
'booking:view'
)
ON CONFLICT DO NOTHING;

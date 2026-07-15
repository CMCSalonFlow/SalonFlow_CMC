INSERT INTO roles(created_at, updated_at, code, name, description)
VALUES
(NOW(), NOW(), 'SUPER_ADMIN', 'Super Admin', 'System Administrator'),
(NOW(), NOW(), 'SALON_OWNER', 'Salon Owner', 'Salon Owner'),
(NOW(), NOW(), 'STAFF', 'Staff', 'Salon Staff'),
(NOW(), NOW(), 'CUSTOMER', 'Customer', 'Customer')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(created_at, updated_at, code, description)
VALUES

(NOW(), NOW(), 'user:create', 'Create user'),
(NOW(), NOW(), 'user:view', 'View user'),
(NOW(), NOW(), 'user:update', 'Update user'),
(NOW(), NOW(), 'user:delete', 'Delete user'),

(NOW(), NOW(), 'salon:create', 'Create salon'),
(NOW(), NOW(), 'salon:view', 'View salon'),
(NOW(), NOW(), 'salon:update', 'Update salon'),
(NOW(), NOW(), 'salon:delete', 'Delete salon'),

(NOW(), NOW(), 'service:create', 'Create service'),
(NOW(), NOW(), 'service:view', 'View service'),
(NOW(), NOW(), 'service:update', 'Update service'),
(NOW(), NOW(), 'service:delete', 'Delete service'),

(NOW(), NOW(), 'booking:create', 'Create booking'),
(NOW(), NOW(), 'booking:view', 'View booking'),
(NOW(), NOW(), 'booking:update', 'Update booking'),
(NOW(), NOW(), 'booking:cancel', 'Cancel booking'),

(NOW(), NOW(), 'staff:create', 'Create staff'),
(NOW(), NOW(), 'staff:view', 'View staff'),
(NOW(), NOW(), 'staff:update', 'Update staff'),
(NOW(), NOW(), 'staff:delete', 'Delete staff'),

(NOW(), NOW(), 'profile:view', 'View profile'),
(NOW(), NOW(), 'profile:update', 'Update profile')
ON CONFLICT (code) DO NOTHING;
-- Gán toàn bộ quyền cho SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- Gán các quyền cơ bản cho SALON_OWNER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'salon:update', 'salon:view',
    'branch:create', 'branch:view', 'branch:update', 'branch:delete',
    'service:create', 'service:view', 'service:update', 'service:delete',
    'category:create', 'category:view', 'category:update', 'category:delete',
    'bundle:create', 'bundle:view', 'bundle:update', 'bundle:delete',
    'booking:create', 'booking:view', 'booking:update', 'booking:cancel',
    'staff:create', 'staff:view', 'staff:update', 'staff:delete',
    'review:view', 'review:reply', 'review:report',
    'ticket:create', 'ticket:view', 'ticket:update', 'ticket:reply',
    'campaign:create', 'campaign:view', 'campaign:update', 'campaign:delete',
    'loyalty:view', 'loyalty:update',
    'notification:create', 'notification:view', 'notification:delete',
    'subscription:create', 'subscription:view', 'subscription:update',
    'analytics:view', 'role:view'
)
WHERE r.code = 'SALON_OWNER'
ON CONFLICT DO NOTHING;

-- Gán quyền cho MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'branch:view', 'salon:view',
    'service:view', 'category:view', 'bundle:view',
    'booking:create', 'booking:view', 'booking:update', 'booking:cancel',
    'staff:view', 'staff:update',
    'review:view', 'review:reply',
    'ticket:create', 'ticket:view', 'ticket:reply',
    'loyalty:view',
    'notification:create', 'notification:view',
    'analytics:view'
)
WHERE r.code = 'MANAGER'
ON CONFLICT DO NOTHING;

-- Gán quyền cho STAFF
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'booking:view', 'booking:update',
    'profile:view', 'profile:update',
    'notification:view'
)
WHERE r.code = 'STAFF'
ON CONFLICT DO NOTHING;

-- Gán quyền cho CUSTOMER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'booking:create', 'booking:view', 'booking:cancel',
    'review:create', 'review:update', 'review:delete',
    'profile:view', 'profile:update',
    'ticket:create', 'ticket:view', 'ticket:reply',
    'notification:view'
)
WHERE r.code = 'CUSTOMER'
ON CONFLICT DO NOTHING;

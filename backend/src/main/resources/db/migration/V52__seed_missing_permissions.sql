INSERT INTO permissions(created_at, updated_at, code, description)
VALUES
-- Role Management
(NOW(), NOW(), 'role:create', 'Create role'),
(NOW(), NOW(), 'role:view', 'View role'),
(NOW(), NOW(), 'role:update', 'Update role'),
(NOW(), NOW(), 'role:delete', 'Delete role'),

-- Branch Management
(NOW(), NOW(), 'branch:create', 'Create branch'),
(NOW(), NOW(), 'branch:view', 'View branch'),
(NOW(), NOW(), 'branch:update', 'Update branch'),
(NOW(), NOW(), 'branch:delete', 'Delete branch'),

-- Category Management
(NOW(), NOW(), 'category:create', 'Create category'),
(NOW(), NOW(), 'category:view', 'View category'),
(NOW(), NOW(), 'category:update', 'Update category'),
(NOW(), NOW(), 'category:delete', 'Delete category'),

-- Bundle Management
(NOW(), NOW(), 'bundle:create', 'Create bundle'),
(NOW(), NOW(), 'bundle:view', 'View bundle'),
(NOW(), NOW(), 'bundle:update', 'Update bundle'),
(NOW(), NOW(), 'bundle:delete', 'Delete bundle'),

-- Review Management
(NOW(), NOW(), 'review:create', 'Create review'),
(NOW(), NOW(), 'review:view', 'View review'),
(NOW(), NOW(), 'review:update', 'Update review'),
(NOW(), NOW(), 'review:delete', 'Delete review'),
(NOW(), NOW(), 'review:reply', 'Reply to review'),
(NOW(), NOW(), 'review:report', 'Report review'),

-- Audit Log Management
(NOW(), NOW(), 'audit:view', 'View audit logs'),

-- Support Ticket Management
(NOW(), NOW(), 'ticket:create', 'Create support ticket'),
(NOW(), NOW(), 'ticket:view', 'View support ticket'),
(NOW(), NOW(), 'ticket:update', 'Update support ticket'),
(NOW(), NOW(), 'ticket:delete', 'Delete support ticket'),
(NOW(), NOW(), 'ticket:reply', 'Reply to support ticket'),

-- Campaign Management
(NOW(), NOW(), 'campaign:create', 'Create campaign'),
(NOW(), NOW(), 'campaign:view', 'View campaign'),
(NOW(), NOW(), 'campaign:update', 'Update campaign'),
(NOW(), NOW(), 'campaign:delete', 'Delete campaign'),

-- Loyalty Points Management
(NOW(), NOW(), 'loyalty:view', 'View loyalty points'),
(NOW(), NOW(), 'loyalty:update', 'Update loyalty points'),

-- Notification Management
(NOW(), NOW(), 'notification:create', 'Create notification'),
(NOW(), NOW(), 'notification:view', 'View notification'),
(NOW(), NOW(), 'notification:delete', 'Delete notification'),

-- Subscription Management
(NOW(), NOW(), 'subscription:create', 'Create subscription'),
(NOW(), NOW(), 'subscription:view', 'View subscription'),
(NOW(), NOW(), 'subscription:update', 'Update subscription'),
(NOW(), NOW(), 'subscription:delete', 'Delete subscription'),

-- Analytics
(NOW(), NOW(), 'analytics:view', 'View analytics')
ON CONFLICT (code) DO NOTHING;

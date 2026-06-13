INSERT INTO users(
username,
email,
password_hash,
full_name
)
VALUES (
'superadmin',
'superadmin@test.com',
'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
'Super Admin'
);

INSERT INTO user_roles(
user_id,
role_id
)
VALUES (
(SELECT id FROM users WHERE username='superadmin'),
(SELECT id FROM roles WHERE name='SUPER_ADMIN')
);

INSERT INTO roles(created_at, updated_at, code, name, description)
VALUES
(NOW(), NOW(), 'MANAGER', 'Manager', 'Salon Manager / Receptionist')
ON CONFLICT (code) DO NOTHING;

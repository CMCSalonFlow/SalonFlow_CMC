-- Tối ưu tra cứu staff theo tài khoản người dùng
CREATE INDEX IF NOT EXISTS idx_staff_user_id
ON staff(user_id);

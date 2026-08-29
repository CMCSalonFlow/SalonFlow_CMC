-- Migration V56: Performance indexes for high-volume tables (Bookings, Shifts, Reviews, Audit Logs)

CREATE INDEX IF NOT EXISTS idx_bookings_branch_date_status ON bookings(branch_id, booking_date, status);
CREATE INDEX IF NOT EXISTS idx_bookings_assigned_staff_date ON bookings(assigned_staff_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_shifts_user_branch_date ON shifts(user_id, branch_id, shift_date);
CREATE INDEX IF NOT EXISTS idx_reviews_salon_created ON reviews(salon_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_action ON audit_logs(created_at, action);

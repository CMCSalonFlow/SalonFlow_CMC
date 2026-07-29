-- =====================================================
-- BỔ SUNG TRẠNG THÁI CHECKED_IN CHO BOOKINGS
-- =====================================================
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS chk_bookings_status;

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));

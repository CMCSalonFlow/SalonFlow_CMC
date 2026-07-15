ALTER TABLE services
ADD COLUMN deposit_required BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE services
ADD COLUMN deposit_percentage DECIMAL(5,2);

ALTER TABLE bookings
ADD COLUMN deposit_amount DECIMAL(12,2);

ALTER TABLE bookings
ADD COLUMN remaining_amount DECIMAL(12,2);

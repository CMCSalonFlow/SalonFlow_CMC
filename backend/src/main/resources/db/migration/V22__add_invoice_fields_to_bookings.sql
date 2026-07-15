ALTER TABLE bookings
ADD COLUMN invoice_url TEXT;

ALTER TABLE bookings
ADD COLUMN invoice_generated_at TIMESTAMP;
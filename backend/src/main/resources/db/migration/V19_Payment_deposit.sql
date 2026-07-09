ALTER TABLE services
ADD COLUMN deposit_required BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE services
ADD COLUMN deposit_percentage DECIMAL(5,2);
-- =====================================================
-- BỔ SUNG CỘT CONFIRMED_BY VÀ NỚI LỎNG HÌNH THỨC THANH TOÁN CHO POS TIỀN MẶT
-- =====================================================
ALTER TABLE payments ADD COLUMN IF NOT EXISTS confirmed_by BIGINT;

DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'chk_payments_method') THEN
        ALTER TABLE payments DROP CONSTRAINT chk_payments_method;
    END IF;
END $$;

ALTER TABLE payments ADD CONSTRAINT chk_payments_method 
    CHECK (payment_method IN ('VNPAY', 'MOMO', 'ZALOPAY', 'CASH', 'PAY_AT_COUNTER'));

-- =====================================================
-- SUBSCRIPTIONS
-- =====================================================

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    salon_id BIGINT NOT NULL,
    plan VARCHAR(50) NOT NULL,
    features JSONB NOT NULL,
    billing_cycle VARCHAR(50) NOT NULL,
    price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    start_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_date TIMESTAMPTZ,
    stripe_subscription_id VARCHAR(255),
    stripe_customer_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_subscriptions_salon
        FOREIGN KEY(salon_id)
        REFERENCES salons(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_subscriptions_salon ON subscriptions(salon_id);

CREATE TRIGGER trg_subscriptions_updated_at
BEFORE UPDATE ON subscriptions
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

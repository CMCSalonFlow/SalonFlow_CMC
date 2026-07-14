CREATE TABLE oauth_accounts (
id BIGSERIAL PRIMARY KEY,
user_id BIGINT NOT NULL,
provider VARCHAR(50) NOT NULL,
provider_user_id VARCHAR(255) NOT NULL,
email VARCHAR(255) NOT NULL,
email_verified BOOLEAN,

created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

CONSTRAINT fk_oauth_accounts_user
    FOREIGN KEY(user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

CONSTRAINT uk_oauth_accounts_provider_user
    UNIQUE(provider, provider_user_id),

CONSTRAINT uk_oauth_accounts_user_provider
    UNIQUE(user_id, provider)
);

CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);
CREATE INDEX idx_oauth_accounts_email ON oauth_accounts(email);

CREATE TRIGGER trg_oauth_accounts_updated_at
BEFORE UPDATE ON oauth_accounts
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

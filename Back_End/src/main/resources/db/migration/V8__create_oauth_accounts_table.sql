CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider VARCHAR(255) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_oauth_accounts_provider_provider_user_id UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_oauth_accounts_tenant_user_provider UNIQUE (tenant_id, user_id, provider)
);

CREATE INDEX idx_oauth_accounts_tenant_id ON oauth_accounts (tenant_id);

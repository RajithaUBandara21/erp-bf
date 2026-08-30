CREATE TABLE memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    organization_id UUID NOT NULL REFERENCES organizations (id),
    location_type VARCHAR(20),
    location_id UUID,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_memberships_user_organization UNIQUE (user_id, organization_id)
);

CREATE INDEX idx_memberships_user_id ON memberships (user_id);
CREATE INDEX idx_memberships_tenant_id ON memberships (tenant_id);
CREATE INDEX idx_memberships_organization_id ON memberships (organization_id);

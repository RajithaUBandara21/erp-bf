CREATE TABLE branches (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_branches_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_branches_tenant_id ON branches (tenant_id);
CREATE INDEX idx_branches_organization_id ON branches (organization_id);

CREATE TABLE stores (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_stores_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_stores_tenant_id ON stores (tenant_id);
CREATE INDEX idx_stores_organization_id ON stores (organization_id);

CREATE TABLE warehouses (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_warehouses_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_warehouses_tenant_id ON warehouses (tenant_id);
CREATE INDEX idx_warehouses_organization_id ON warehouses (organization_id);

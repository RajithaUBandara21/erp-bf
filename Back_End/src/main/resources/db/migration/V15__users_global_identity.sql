-- Global identity (feature 5a.2): a User is now a platform-wide account. Organization
-- scope lives on Membership, not here. Fails loudly if an email was reused across
-- tenants - resolve any duplicate by hand before running (dev data only).
ALTER TABLE users DROP CONSTRAINT uq_users_tenant_email;

-- Dropping these columns cascades their supporting indexes (idx_users_tenant_id,
-- idx_users_organization_id) and foreign keys.
ALTER TABLE users DROP COLUMN tenant_id;
ALTER TABLE users DROP COLUMN organization_id;

ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

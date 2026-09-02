-- OAuth account linking is a global-user concern after feature 5 (F-04): a linked Google
-- identity belongs to the User, not to whichever Organization the session happened to be
-- scoped to at link time. Drop the tenant dimension so link/status/unlink resolve by
-- user_id + provider alone. A pre-existing pair of rows sharing (user_id, provider) across
-- two tenants violates the new constraint - resolve any duplicate by hand first (dev data only).
ALTER TABLE oauth_accounts DROP CONSTRAINT uq_oauth_accounts_tenant_user_provider;
DROP INDEX idx_oauth_accounts_tenant_id;
ALTER TABLE oauth_accounts DROP COLUMN tenant_id;

ALTER TABLE oauth_accounts ADD CONSTRAINT uq_oauth_accounts_user_provider UNIQUE (user_id, provider);

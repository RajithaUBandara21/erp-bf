-- A Session is an account/device record for a global User after feature 5 (F-09), not tenant-owned
-- data: its tenant_id was mutated on every in-session organization switch, so the account-level
-- session list / revoke endpoints silently hid a user's devices that had switched to another Tenant.
-- Drop the tenant dimension so every session lookup keys on user_id alone. The inline REFERENCES
-- tenants (id) foreign key drops together with the column.
DROP INDEX idx_sessions_tenant_id;
ALTER TABLE sessions DROP COLUMN tenant_id;

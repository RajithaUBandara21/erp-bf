-- Feature 6a: a Platform Super Admin has no tenant, so an audit event for its own actions
-- (starting with the bootstrap event itself) has no tenant_id to record. Existing
-- tenant-scoped audit queries always pass a concrete tenant_id, so this is safe for them.
ALTER TABLE audit_logs ALTER COLUMN tenant_id DROP NOT NULL;

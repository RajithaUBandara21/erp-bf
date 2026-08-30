INSERT INTO memberships (id, user_id, tenant_id, organization_id, location_type, location_id, status, created_at, updated_at)
SELECT gen_random_uuid(), id, tenant_id, organization_id, NULL, NULL, 'ACTIVE', created_at, updated_at
FROM users;

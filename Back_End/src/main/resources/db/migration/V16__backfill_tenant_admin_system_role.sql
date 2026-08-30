-- Tenant Admin system role (feature 5b.2): every existing tenant gains a fourth
-- system-managed role with the full permission catalogue, assigned to whichever
-- Membership currently holds that tenant's Owner role. New tenants get this from
-- SystemRoleProvisioner instead. Fails loudly (uq_roles_tenant_name) if a tenant
-- already has a role literally named 'Tenant Admin' - resolve by hand (dev data).

INSERT INTO roles (id, tenant_id, name, description, system_managed, created_at, updated_at)
SELECT gen_random_uuid(), t.id, 'Tenant Admin',
       'Owner-level access to every Organization under the Tenant, including billing and subscription.',
       TRUE, now(), now()
FROM tenants t;

INSERT INTO role_permissions (id, role_id, permission_id, created_at, updated_at)
SELECT gen_random_uuid(), r.id, p.id, now(), now()
FROM roles r
CROSS JOIN permissions p
WHERE r.system_managed = TRUE
  AND r.name = 'Tenant Admin';

INSERT INTO user_roles (id, tenant_id, membership_id, role_id, created_at, updated_at)
SELECT gen_random_uuid(), owner_assignment.tenant_id, owner_assignment.membership_id, tenant_admin_role.id,
       now(), now()
FROM user_roles owner_assignment
JOIN roles owner_role
  ON owner_role.id = owner_assignment.role_id
 AND owner_role.system_managed = TRUE
 AND owner_role.name = 'Owner'
JOIN roles tenant_admin_role
  ON tenant_admin_role.tenant_id = owner_role.tenant_id
 AND tenant_admin_role.system_managed = TRUE
 AND tenant_admin_role.name = 'Tenant Admin';

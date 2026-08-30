ALTER TABLE user_roles ADD COLUMN membership_id UUID;

UPDATE user_roles ur
SET membership_id = m.id
FROM memberships m
WHERE m.user_id = ur.user_id
  AND m.tenant_id = ur.tenant_id;

ALTER TABLE user_roles ALTER COLUMN membership_id SET NOT NULL;
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_membership
    FOREIGN KEY (membership_id) REFERENCES memberships (id) ON DELETE CASCADE;

ALTER TABLE user_roles DROP CONSTRAINT uq_user_roles_user_role;
ALTER TABLE user_roles ADD CONSTRAINT uq_user_roles_membership_role UNIQUE (membership_id, role_id);

DROP INDEX idx_user_roles_user_id;
ALTER TABLE user_roles DROP COLUMN user_id;

CREATE INDEX idx_user_roles_membership_id ON user_roles (membership_id);

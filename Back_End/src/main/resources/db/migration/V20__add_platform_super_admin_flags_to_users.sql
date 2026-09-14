-- Feature 6a: groundwork for the Platform Super Admin (project-overview.md). Modeled as a
-- plain boolean checked directly, not a Role/Permission grant - a Super Admin has no
-- Membership, so there is no membership_id for the generic permission resolver to key on.
ALTER TABLE users ADD COLUMN platform_super_admin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

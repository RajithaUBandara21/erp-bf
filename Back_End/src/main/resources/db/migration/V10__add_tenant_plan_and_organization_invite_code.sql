ALTER TABLE tenants ADD COLUMN plan VARCHAR(255);
ALTER TABLE tenants ADD COLUMN max_organizations INTEGER NOT NULL DEFAULT 1;

ALTER TABLE organizations ADD COLUMN invite_code VARCHAR(255);
ALTER TABLE organizations ADD CONSTRAINT uq_organizations_invite_code UNIQUE (invite_code);

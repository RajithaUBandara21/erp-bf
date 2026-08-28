CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

-- The grantable-capability catalog: one row per resource x action. It describes
-- the target module set, so several resources have no module behind them yet
-- (audit is build-plan 2, notification is 17, billing has no MVP feature).
-- New modules add rows in a later migration.
INSERT INTO permissions (id, code, resource, action, created_at, updated_at)
SELECT gen_random_uuid(),
       r.resource || '.' || lower(a.action),
       r.resource,
       a.action,
       now(),
       now()
FROM (VALUES
    ('product'), ('inventory'), ('purchasing'), ('sales'), ('pos'),
    ('ecommerce'), ('customer'), ('supplier'), ('shipping'), ('payment'),
    ('accounting'), ('promotion'), ('reporting'), ('notification'), ('audit'),
    ('user'), ('role'), ('organization'), ('billing')
) AS r (resource)
CROSS JOIN (VALUES
    ('VIEW'), ('CREATE'), ('EDIT'), ('DELETE'), ('APPROVE')
) AS a (action);

-- Invite code lifecycle (feature 5c.1): every Organization needs a rotatable
-- invite_code for employee self-join (5c.2). V10 added the nullable column and
-- its UNIQUE constraint; new Organizations get a code from OrganizationServiceImpl
-- now, and this backfills the rows that predate that. random() is fine here: a
-- one-time backfill of pre-existing rows, and any admin can rotate afterwards -
-- the runtime path uses SecureRandom.

DO $$
DECLARE
    org RECORD;
    new_code TEXT;
    alphabet TEXT := 'ABCDEFGHJKMNPQRSTVWXYZ0123456789';
    i INT;
BEGIN
    FOR org IN SELECT id FROM organizations WHERE invite_code IS NULL LOOP
        LOOP
            new_code := '';
            FOR i IN 1..10 LOOP
                new_code := new_code || substr(alphabet, 1 + floor(random() * length(alphabet))::int, 1);
            END LOOP;
            EXIT WHEN NOT EXISTS (SELECT 1 FROM organizations WHERE invite_code = new_code);
        END LOOP;
        UPDATE organizations SET invite_code = new_code WHERE id = org.id;
    END LOOP;
END $$;

ALTER TABLE organizations ALTER COLUMN invite_code SET NOT NULL;

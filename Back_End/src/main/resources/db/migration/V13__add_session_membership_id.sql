ALTER TABLE sessions ADD COLUMN membership_id UUID;

UPDATE sessions s
SET membership_id = m.id
FROM memberships m
WHERE m.user_id = s.user_id;

ALTER TABLE sessions ALTER COLUMN membership_id SET NOT NULL;
ALTER TABLE sessions ADD CONSTRAINT fk_sessions_membership
    FOREIGN KEY (membership_id) REFERENCES memberships (id);

CREATE INDEX idx_sessions_membership_id ON sessions (membership_id);

CREATE TABLE families (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT username_normalized CHECK (username = lower(username))
);

CREATE TABLE family_memberships (
    user_id UUID PRIMARY KEY REFERENCES app_users(id),
    family_id UUID NOT NULL REFERENCES families(id),
    role VARCHAR(10) NOT NULL CHECK (role IN ('MEMBER', 'ADMIN'))
);
CREATE INDEX memberships_family_idx ON family_memberships(family_id);

CREATE TABLE identity_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id UUID,
    target_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

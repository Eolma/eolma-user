CREATE TABLE IF NOT EXISTS member (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    nickname        VARCHAR(50) UNIQUE,
    profile_image   VARCHAR(500),
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_member_email ON member(email);
CREATE INDEX IF NOT EXISTS idx_member_nickname ON member(nickname);

CREATE TABLE IF NOT EXISTS social_account (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id       BIGINT NOT NULL REFERENCES member(id),
    provider        VARCHAR(20) NOT NULL,
    provider_id     VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (provider, provider_id)
);

CREATE INDEX IF NOT EXISTS idx_social_account_member ON social_account(member_id);

CREATE TABLE IF NOT EXISTS processed_event (
    event_id     VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

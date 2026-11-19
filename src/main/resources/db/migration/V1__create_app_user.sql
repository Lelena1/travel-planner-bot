CREATE TABLE app_user (
    telegram_id BIGINT      PRIMARY KEY,
    role        VARCHAR(16) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

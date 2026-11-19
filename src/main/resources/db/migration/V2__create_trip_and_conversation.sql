-- Поездка. Пока пользователь отвечает на вопросы бота, поездка хранится как черновик (DRAFT).
CREATE TABLE trip (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app_user (telegram_id) ON DELETE CASCADE,
    status      VARCHAR(16)  NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE')),
    destination VARCHAR(200),
    origin      VARCHAR(100),
    date_from   DATE,
    date_to     DATE,
    adults      INTEGER      CHECK (adults BETWEEN 1 AND 20),
    children    INTEGER      CHECK (children BETWEEN 0 AND 20),
    child_ages  VARCHAR(100),
    pets        VARCHAR(16),
    budget      VARCHAR(16),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT trip_dates_order CHECK (date_to IS NULL OR date_from IS NULL OR date_to >= date_from)
);

CREATE INDEX trip_user_status_idx ON trip (user_id, status);

-- Текущий диалог пользователя с ботом: на каком шаге анкеты он находится.
CREATE TABLE conversation (
    user_id    BIGINT      PRIMARY KEY REFERENCES app_user (telegram_id) ON DELETE CASCADE,
    step       VARCHAR(32) NOT NULL,
    trip_id    BIGINT      REFERENCES trip (id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

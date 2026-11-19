-- Подписка на цену билетов для поездки: бот периодически проверяет цену и сообщает о снижении.
CREATE TABLE price_watch (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES app_user (telegram_id) ON DELETE CASCADE,
    chat_id          BIGINT       NOT NULL,
    trip_id          BIGINT       NOT NULL REFERENCES trip (id) ON DELETE CASCADE,
    origin_code      VARCHAR(3)   NOT NULL,
    destination_code VARCHAR(3)   NOT NULL,
    route_title      VARCHAR(200) NOT NULL,
    last_price       INTEGER,
    min_price        INTEGER,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_checked_at  TIMESTAMPTZ,
    CONSTRAINT price_watch_one_per_trip UNIQUE (trip_id)
);

CREATE INDEX price_watch_user_idx ON price_watch (user_id);

-- История цен: одна запись на каждую проверку. Пригодится для графика цен.
CREATE TABLE price_snapshot (
    id         BIGSERIAL   PRIMARY KEY,
    watch_id   BIGINT      NOT NULL REFERENCES price_watch (id) ON DELETE CASCADE,
    price      INTEGER,
    checked_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX price_snapshot_watch_idx ON price_snapshot (watch_id, checked_at);

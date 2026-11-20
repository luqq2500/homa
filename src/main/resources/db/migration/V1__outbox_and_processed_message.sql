-- Slice 0: cross-context delivery and idempotent handling (docs/decisions/0002).

-- The transactional outbox. A context's IE publisher writes a row in the same transaction as the aggregate
-- change; the relay in com.homa.app delivers unpublished rows in id order per aggregate.
CREATE TABLE outbox_event (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- the ordering sequence
    event_id        TEXT        NOT NULL UNIQUE,
    event_type      TEXT        NOT NULL,                            -- simple name of a com.homa.ie.events record
    aggregate_type  TEXT        NOT NULL,                            -- the event's aggregate component, e.g. reservationId
    aggregate_id    TEXT        NOT NULL,
    payload         JSONB       NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,                                     -- null until every listener succeeded
    attempts        INTEGER     NOT NULL DEFAULT 0,                  -- failed deliveries so far
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),              -- backoff after a failed delivery
    last_error      TEXT
);

-- The relay's claim: due rows in order, and "is there an earlier unpublished row for this aggregate?".
CREATE INDEX outbox_event_unpublished ON outbox_event (id) WHERE published_at IS NULL;
CREATE INDEX outbox_event_unpublished_by_aggregate
    ON outbox_event (aggregate_type, aggregate_id, id) WHERE published_at IS NULL;

-- One row per (event, handler) a consuming context has handled, written in the handler's own transaction
-- through that context's ProcessedMessageStore. A repeat insert does nothing, so a redelivery is a no-op.
CREATE TABLE processed_message (
    event_id     TEXT        NOT NULL,
    handler      TEXT        NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, handler)
);

-- Test only: the tables of the sample contexts in com.homa.app.outbox.sample.

-- The publishing context's aggregate.
CREATE TABLE sample_customer (
    customer_id TEXT PRIMARY KEY,
    city        TEXT NOT NULL
);

-- The consuming contexts' effects. times_recorded counts how often the effect ran, so a duplicate shows.
CREATE TABLE sample_customer_standing (
    customer_id    TEXT PRIMARY KEY,
    city           TEXT    NOT NULL,
    times_recorded INTEGER NOT NULL
);

CREATE TABLE sample_conversation_member (
    customer_id    TEXT PRIMARY KEY,
    times_recorded INTEGER NOT NULL
);

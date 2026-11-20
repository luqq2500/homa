package com.homa.app.outbox;

/** An unpublished outbox row, claimed by a relay. */
record OutboxEntry(long id, String eventId, String eventType, String payload) {
}

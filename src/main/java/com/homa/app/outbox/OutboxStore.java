package com.homa.app.outbox;

import com.homa.ie.port.IntegrationEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The outbox_event table (db/migration/V1__outbox_and_processed_message.sql). Every statement joins the caller's
 * transaction.
 */
@Repository
public class OutboxStore {

    /** A failed row waits 2, 4, 8 ... seconds before its next attempt, never longer than this. */
    static final int MAX_BACKOFF_SECONDS = 300;

    /**
     * The oldest due row whose aggregate has no earlier unpublished row, locked for this transaction. Rows another
     * relay holds are skipped, and so are the later rows of the aggregate it holds, so delivery stays in order per
     * aggregate however many relays run.
     */
    private static final String CLAIM_NEXT_DUE = """
            SELECT o.id, o.event_id, o.event_type, o.payload::text AS payload
            FROM outbox_event o
            WHERE o.published_at IS NULL
              AND o.next_attempt_at <= now()
              AND NOT EXISTS (
                  SELECT 1 FROM outbox_event earlier
                  WHERE earlier.aggregate_type = o.aggregate_type
                    AND earlier.aggregate_id = o.aggregate_id
                    AND earlier.published_at IS NULL
                    AND earlier.id < o.id)
            ORDER BY o.id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """;

    private final JdbcClient jdbc;

    public OutboxStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    void append(IntegrationEvent event, String eventType, String payload) {
        AggregateKey aggregate = AggregateKey.of(event);
        jdbc.sql("""
                INSERT INTO outbox_event (event_id, event_type, aggregate_type, aggregate_id, payload, occurred_at)
                VALUES (:eventId, :eventType, :aggregateType, :aggregateId, CAST(:payload AS jsonb), :occurredAt)
                """)
                .param("eventId", event.eventId())
                .param("eventType", eventType)
                .param("aggregateType", aggregate.type())
                .param("aggregateId", aggregate.id())
                .param("payload", payload)
                .param("occurredAt", OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                .update();
    }

    Optional<OutboxEntry> claimNextDue() {
        return jdbc.sql(CLAIM_NEXT_DUE)
                .query((row, n) -> new OutboxEntry(row.getLong("id"), row.getString("event_id"),
                        row.getString("event_type"), row.getString("payload")))
                .optional();
    }

    void markPublished(long id) {
        jdbc.sql("UPDATE outbox_event SET published_at = now() WHERE id = :id").param("id", id).update();
    }

    void recordFailure(long id, String error) {
        jdbc.sql("""
                UPDATE outbox_event
                SET attempts = attempts + 1,
                    last_error = left(:error, 2000),
                    next_attempt_at = now() + make_interval(secs => least(power(2, attempts + 1), :maxBackoff))
                WHERE id = :id AND published_at IS NULL
                """)
                .param("id", id)
                .param("error", error)
                .param("maxBackoff", MAX_BACKOFF_SECONDS)
                .update();
    }
}

package com.homa.app.outbox.sample;

import static org.assertj.core.api.Assertions.assertThat;

import com.homa.app.outbox.TechnicalRowPurge;
import com.homa.app.outbox.TechnicalRowPurge.PurgedRows;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Delivered outbox rows and processed-message keys go after 30 days; undelivered rows stay whatever their age. */
@OutboxSampleTest
class TechnicalRowPurgeIT extends OutboxSampleSupport {

    private static final Duration THIRTY_DAYS = Duration.ofDays(30);

    @Autowired
    TechnicalRowPurge purge;

    private void age(String table, String column, String eventId, String age) {
        jdbc.sql("UPDATE " + table + " SET " + column + " = now() - CAST(:age AS interval) WHERE event_id = :id")
                .param("age", age).param("id", eventId).update();
    }

    @Test
    @DisplayName("A delivered outbox row and its processed-message keys older than 30 days are deleted.")
    void deliveredRowsAndKeysOlderThanThirtyDaysAreDeleted() {
        String eventId = register(AISYAH);
        relayEverythingDue();
        age("outbox_event", "published_at", eventId, "30 days 1 minute");
        age("processed_message", "processed_at", eventId, "30 days 1 minute");
        int keys = count("SELECT count(*) FROM processed_message WHERE event_id = '" + eventId + "'");

        PurgedRows purged = purge.purgeOlderThan(THIRTY_DAYS);

        assertThat(keys).isPositive();
        assertThat(purged).isEqualTo(new PurgedRows(1, keys));
        assertThat(count("SELECT count(*) FROM outbox_event")).isZero();
        assertThat(count("SELECT count(*) FROM processed_message")).isZero();
    }

    @Test
    @DisplayName("Rows delivered or handled less than 30 days ago are kept.")
    void rowsYoungerThanThirtyDaysAreKept() {
        String eventId = register(AISYAH);
        relayEverythingDue();
        age("outbox_event", "published_at", eventId, "29 days 23 hours");
        age("processed_message", "processed_at", eventId, "29 days 23 hours");

        PurgedRows purged = purge.purgeOlderThan(THIRTY_DAYS);

        assertThat(purged).isEqualTo(new PurgedRows(0, 0));
        assertThat(count("SELECT count(*) FROM outbox_event")).isOne();
    }

    @Test
    @DisplayName("An outbox row not yet delivered is never purged, however old it is, so it still reaches its consumers.")
    void undeliveredRowIsNeverPurged() {
        String eventId = register(AISYAH);
        age("outbox_event", "recorded_at", eventId, "90 days");

        purge.purgeOlderThan(THIRTY_DAYS);

        assertThat(count("SELECT count(*) FROM outbox_event WHERE published_at IS NULL")).isOne();
        assertThat(relayEverythingDue()).isOne();
    }

    @Test
    @DisplayName("Running the purge again deletes nothing more.")
    void purgeIsSafeToRerun() {
        String eventId = register(AISYAH);
        relayEverythingDue();
        age("outbox_event", "published_at", eventId, "31 days");

        purge.purgeOlderThan(THIRTY_DAYS);

        assertThat(purge.purgeOlderThan(THIRTY_DAYS)).isEqualTo(new PurgedRows(0, 0));
    }
}

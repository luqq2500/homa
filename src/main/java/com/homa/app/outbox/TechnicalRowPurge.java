package com.homa.app.outbox;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes technical rows that carry only ids once they are older than the retention: delivered outbox rows and
 * ProcessedMessageStore keys (plan, "Data retention": plumbing, not a business rule). An undelivered outbox row is
 * never purged, whatever its age, because it still has to reach its consumers. Ages are measured on the database's
 * clock, the same clock that stamped the rows. Safe to run on every instance and to rerun after a crash.
 */
@Component
public class TechnicalRowPurge {

    private static final Logger LOG = LoggerFactory.getLogger(TechnicalRowPurge.class);

    private final JdbcClient jdbc;

    public TechnicalRowPurge(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Deletes rows older than {@code retention}; returns how many outbox rows and keys were deleted. */
    @Transactional
    public PurgedRows purgeOlderThan(Duration retention) {
        long seconds = retention.toSeconds();
        int outboxRows = jdbc.sql("""
                DELETE FROM outbox_event
                WHERE published_at IS NOT NULL AND published_at < now() - make_interval(secs => :seconds)
                """).param("seconds", seconds).update();
        int processedKeys = jdbc.sql("""
                DELETE FROM processed_message WHERE processed_at < now() - make_interval(secs => :seconds)
                """).param("seconds", seconds).update();
        PurgedRows purged = new PurgedRows(outboxRows, processedKeys);
        LOG.info("Purged technical rows older than {}: {}", retention, purged);
        return purged;
    }

    /** What one purge deleted. */
    public record PurgedRows(int outboxRows, int processedMessageKeys) {
    }
}

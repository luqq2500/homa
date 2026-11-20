package com.homa.app.outbox.sample.conversation.infra.persistent;

import com.homa.app.outbox.sample.conversation.usecase.port.out.ProcessedMessageStore;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The pattern every consuming context copies into its own infra/persistent: one insert into the shared
 * processed_message table, and "was a row inserted?" is "is this the first handling?". A concurrent duplicate
 * waits on the primary key and then inserts nothing.
 */
public class JdbcProcessedMessageStore implements ProcessedMessageStore {

    private final JdbcClient jdbc;

    public JdbcProcessedMessageStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean recordFirstHandling(String eventId, String handler) {
        return jdbc.sql("""
                INSERT INTO processed_message (event_id, handler) VALUES (:eventId, :handler)
                ON CONFLICT (event_id, handler) DO NOTHING
                """)
                .param("eventId", eventId)
                .param("handler", handler)
                .update() == 1;
    }
}

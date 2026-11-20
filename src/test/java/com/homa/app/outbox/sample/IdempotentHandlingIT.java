package com.homa.app.outbox.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every IE handler is idempotent: its ProcessedMessageStore records (eventId, handler) in the handler's own
 * transaction, so a redelivered event reaches the handler's effect exactly once.
 */
@OutboxSampleTest
class IdempotentHandlingIT extends OutboxSampleSupport {

    @Test
    @DisplayName("A redelivered event reaches each handler's effect exactly once.")
    void redeliveredEventTakesEffectOnce() {
        String eventId = register(AISYAH);
        relayEverythingDue();
        // As if the relay had stopped after the handlers committed and before it marked the row published.
        jdbc.sql("UPDATE outbox_event SET published_at = NULL WHERE event_id = :eventId").param("eventId", eventId)
                .update();

        relayEverythingDue();

        assertThat(log.eventIdsReceivedBy("standing")).containsExactly(eventId, eventId);
        assertThat(timesRecorded("sample_customer_standing", AISYAH)).isOne();
        assertThat(timesRecorded("sample_conversation_member", AISYAH)).isOne();
    }

    @Test
    @DisplayName("Each handler records its own handling of the event.")
    void eachHandlerRecordsItsOwnHandling() {
        String eventId = register(AISYAH);

        relayEverythingDue();

        List<String> handlers = jdbc.sql("SELECT handler FROM processed_message WHERE event_id = :eventId ORDER BY 1")
                .param("eventId", eventId).query(String.class).list();
        assertThat(handlers).containsExactly("conversation.CustomerRegistered", "standing.CustomerRegistered");
    }

    @Test
    @DisplayName("A handler whose effect fails forgets that it handled the event, so the retry takes effect.")
    void failedHandlingIsNotRecorded() {
        String eventId = register(AISYAH);
        failures.failFor("standing", AISYAH);

        relayEverythingDue();

        assertThat(count("SELECT count(*) FROM processed_message")).isZero();
        assertThat(timesRecorded("sample_customer_standing", AISYAH)).isZero();

        failures.clear();
        endBackoff();
        relayEverythingDue();

        assertThat(outboxRow(eventId).get("published_at")).isNotNull();
        assertThat(timesRecorded("sample_customer_standing", AISYAH)).isOne();
        assertThat(timesRecorded("sample_conversation_member", AISYAH)).isOne();
    }
}

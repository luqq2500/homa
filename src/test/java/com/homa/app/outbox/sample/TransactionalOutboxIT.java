package com.homa.app.outbox.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.IllegalTransactionStateException;

/** A context publishes an integration event in the same transaction as the aggregate change it reports. */
@OutboxSampleTest
class TransactionalOutboxIT extends OutboxSampleSupport {

    @Test
    @DisplayName("A published event is recorded in the outbox with the change, ordered by its aggregate.")
    void publishedEventIsRecordedWithTheChange() {
        String eventId = register(AISYAH);

        Map<String, Object> row = outboxRow(eventId);
        assertThat(count("SELECT count(*) FROM sample_customer")).isOne();
        assertThat(row).containsEntry("event_type", "CustomerRegisteredIE")
                .containsEntry("aggregate_type", "customerId")
                .containsEntry("aggregate_id", AISYAH)
                .containsEntry("city", "SALAK_TINGGI")
                .containsEntry("attempts", 0);
        assertThat(row.get("published_at")).isNull();
    }

    @Test
    @DisplayName("When the publisher's transaction rolls back, the event goes with the change.")
    void rolledBackChangeLeavesNoEvent() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            registerCustomer.register("event-1", AISYAH, "SALAK_TINGGI", Instant.now());
            throw new IllegalStateException("the usecase failed after publishing");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(count("SELECT count(*) FROM sample_customer")).isZero();
        assertThat(count("SELECT count(*) FROM outbox_event")).isZero();
    }

    @Test
    @DisplayName("An event published outside a transaction is refused, so it can never outlive its change.")
    void publishingOutsideATransactionIsRefused() {
        assertThatThrownBy(() -> registerCustomer.register("event-1", AISYAH, "SALAK_TINGGI", Instant.now()))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(count("SELECT count(*) FROM outbox_event")).isZero();
    }

    @Test
    @DisplayName("Inside its own context an event is handled in the publisher's transaction; other contexts wait.")
    void onlyThePublishingContextHandlesTheEventInItsTransaction() {
        register(AISYAH);

        assertThat(log.receivedBy("registration")).singleElement()
                .extracting(DeliveryLog.Delivery::inTransaction).isEqualTo(true);
        assertThat(log.receivedBy("standing")).isEmpty();
        assertThat(log.receivedBy("conversation")).isEmpty();
    }
}

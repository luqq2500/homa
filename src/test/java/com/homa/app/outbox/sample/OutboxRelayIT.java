package com.homa.app.outbox.sample;

import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.PaymentHeldIE;
import com.homa.kernel.Money;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The relay delivers the outbox to consuming contexts: at least once, in order per aggregate, from any instance. */
@OutboxSampleTest
class OutboxRelayIT extends OutboxSampleSupport {

    @Test
    @DisplayName("A relayed event reaches every consuming context once and is then marked published.")
    void relayDeliversToEveryConsumerAndMarksTheEventPublished() {
        String eventId = register(AISYAH);

        int relayed = relayEverythingDue();

        assertThat(relayed).isOne();
        assertThat(log.eventIdsReceivedBy("standing")).containsExactly(eventId);
        assertThat(log.eventIdsReceivedBy("conversation")).containsExactly(eventId);
        assertThat(outboxRow(eventId).get("published_at")).isNotNull();
        assertThat(relayEverythingDue()).isZero();
    }

    @Test
    @DisplayName("The relayed delivery never reaches the publishing context's own listener a second time.")
    void relayedEventDoesNotReachThePublishersOwnListener() {
        register(AISYAH);

        relayEverythingDue();

        assertThat(log.receivedBy("registration")).hasSize(1);
    }

    @Test
    @DisplayName("A relayed event reaches only the listeners for its own event type.")
    void relayedEventReachesOnlyListenersForItsType() {
        String registered = register(AISYAH);
        transaction.executeWithoutResult(status -> events.publishEvent(new PaymentHeldIE("payment-event-1",
                "checkout-1", Money.myr("200"), Instant.now(), Instant.now())));

        relayEverythingDue();

        assertThat(log.eventIdsReceivedBy("conversation:other-type")).containsExactly("payment-event-1");
        assertThat(log.eventIdsReceivedBy("conversation")).containsExactly(registered);
    }

    @Test
    @DisplayName("A failed delivery stays in the outbox and is retried after a backoff; the consumer that "
            + "succeeded keeps its effect.")
    void failedDeliveryIsRetriedAfterABackoff() {
        String eventId = register(AISYAH);
        failures.failFor("conversation", AISYAH);

        relayEverythingDue();

        Map<String, Object> failed = outboxRow(eventId);
        assertThat(failed.get("published_at")).isNull();
        assertThat(failed).containsEntry("attempts", 1).containsEntry("backing_off", true);
        assertThat((String) failed.get("last_error")).contains("simulated");
        assertThat(timesRecorded("sample_customer_standing", AISYAH)).isOne();
        assertThat(relayEverythingDue()).as("not due during its backoff").isZero();

        failures.clear();
        endBackoff();
        relayEverythingDue();

        assertThat(outboxRow(eventId).get("published_at")).isNotNull();
        assertThat(timesRecorded("sample_customer_standing", AISYAH)).isOne();
        assertThat(timesRecorded("sample_conversation_member", AISYAH)).isOne();
    }

    @Test
    @DisplayName("Later events about the same aggregate wait behind a failed one; other aggregates carry on.")
    void laterEventsOfTheSameAggregateWaitBehindAFailedOne() {
        String first = register(AISYAH);
        String second = publishAnotherAbout(AISYAH);
        String farah = register(FARAH);
        failures.failFor("standing", AISYAH);

        relayEverythingDue();

        assertThat(log.eventIdsReceivedBy("standing")).containsExactly(first, farah);
        assertThat(outboxRow(second).get("attempts")).isEqualTo(0);

        failures.clear();
        endBackoff();
        relayEverythingDue();

        assertThat(log.eventIdsReceivedBy("standing")).containsExactly(first, farah, first, second);
        assertThat(count("SELECT count(*) FROM outbox_event WHERE published_at IS NULL")).isZero();
    }

    @Test
    @DisplayName("Relays running at once deliver every event exactly once, in order per aggregate.")
    void concurrentRelaysDeliverEachEventOnceInOrderPerAggregate() throws Exception {
        Map<String, List<String>> publishedInOrder = new LinkedHashMap<>();
        for (int round = 0; round < 8; round++) {
            for (int customer = 0; customer < 5; customer++) {
                String customerId = "customer-" + customer;
                publishedInOrder.computeIfAbsent(customerId, id -> new ArrayList<>())
                        .add(publishAnotherAbout(customerId));
            }
        }

        runRelaysAtOnce(4);

        for (String consumer : List.of("standing", "conversation")) {
            Map<String, List<String>> received = log.receivedBy(consumer).stream().collect(Collectors.groupingBy(
                    DeliveryLog.Delivery::customerId, LinkedHashMap::new,
                    Collectors.mapping(DeliveryLog.Delivery::eventId, Collectors.toList())));
            assertThat(received).as(consumer).containsExactlyInAnyOrderEntriesOf(publishedInOrder);
        }
    }

    @Test
    @DisplayName("A row the relay cannot read is held back and reported, never dropped.")
    void unreadableRowIsHeldBackNotDropped() {
        jdbc.sql("""
                INSERT INTO outbox_event (event_id, event_type, aggregate_type, aggregate_id, payload, occurred_at)
                VALUES ('event-x', 'NoSuchThingIE', 'customerId', 'customer-x', '{}', now())
                """).update();

        relayEverythingDue();

        Map<String, Object> row = outboxRow("event-x");
        assertThat(row.get("published_at")).isNull();
        assertThat(row).containsEntry("attempts", 1);
        assertThat((String) row.get("last_error")).contains("NoSuchThingIE");
    }

    private void runRelaysAtOnce(int relays) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        ExecutorService pool = Executors.newFixedThreadPool(relays);
        try {
            List<Future<?>> running = new ArrayList<>();
            for (int i = 0; i < relays; i++) {
                running.add(pool.submit(() -> {
                    while (unpublished() > 0 && Instant.now().isBefore(deadline)) {
                        relay.relayDue(3);
                    }
                }));
            }
            for (Future<?> relayRun : running) {
                relayRun.get();
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(unpublished()).isZero();
    }

    private int unpublished() {
        return count("SELECT count(*) FROM outbox_event WHERE published_at IS NULL");
    }
}

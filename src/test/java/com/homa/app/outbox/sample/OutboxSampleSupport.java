package com.homa.app.outbox.sample;

import com.homa.app.outbox.OutboxRelay;
import com.homa.app.outbox.sample.registration.usecase.RegisterCustomer;
import com.homa.ie.events.CustomerRegisteredIE;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** Test only: the moves the sample ITs share, on a clean database for every test. */
abstract class OutboxSampleSupport {

    static final String AISYAH = "customer-aisyah";
    static final String FARAH = "customer-farah";

    @Autowired
    JdbcClient jdbc;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    OutboxRelay relay;

    @Autowired
    RegisterCustomer registerCustomer;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    DeliveryLog log;

    @Autowired
    FailureSwitch failures;

    @BeforeEach
    void startClean() {
        jdbc.sql("TRUNCATE outbox_event, processed_message, sample_customer, sample_customer_standing, "
                + "sample_conversation_member").update();
        log.clear();
        failures.clear();
    }

    /** Registers a customer through the sample publishing context, in one transaction; returns the event id. */
    String register(String customerId) {
        String eventId = UUID.randomUUID().toString();
        transaction.executeWithoutResult(status -> registerCustomer.register(eventId, customerId, "SALAK_TINGGI",
                Instant.now()));
        return eventId;
    }

    /** Publishes one more event about an existing customer, bypassing the usecase, in its own transaction. */
    String publishAnotherAbout(String customerId) {
        String eventId = UUID.randomUUID().toString();
        transaction.executeWithoutResult(status -> events.publishEvent(
                new CustomerRegisteredIE(eventId, customerId, "CYBERJAYA", "1, Jalan Sample",
                        "Ring the bell", "ACTIVE", Instant.now())));
        return eventId;
    }

    int relayEverythingDue() {
        return relay.relayDue(1_000);
    }

    Map<String, Object> outboxRow(String eventId) {
        return jdbc.sql("""
                SELECT event_type, aggregate_type, aggregate_id, payload ->> 'city' AS city, published_at, attempts,
                       last_error, next_attempt_at > now() AS backing_off
                FROM outbox_event WHERE event_id = :eventId
                """).param("eventId", eventId).query().singleRow();
    }

    /** Makes every row that is backing off due now, instead of waiting out its backoff. */
    void endBackoff() {
        jdbc.sql("UPDATE outbox_event SET next_attempt_at = now() WHERE published_at IS NULL").update();
    }

    int count(String sql) {
        return jdbc.sql(sql).query(Integer.class).single();
    }

    int timesRecorded(String table, String customerId) {
        return jdbc.sql("SELECT coalesce(max(times_recorded), 0) FROM " + table + " WHERE customer_id = :id")
                .param("id", customerId).query(Integer.class).single();
    }
}

package com.homa.app.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the relay inside the long-running container (docs/decisions/0001): every
 * {@code homa.outbox.relay.fixed-delay} after the last run ended, up to {@code homa.outbox.relay.batch-size} rows.
 * Every instance runs it; the relay's row locks keep them from delivering the same row. Turned off with
 * {@code homa.outbox.relay.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "homa.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelaySchedule {

    private final OutboxRelay relay;
    private final int batchSize;

    public OutboxRelaySchedule(OutboxRelay relay, @Value("${homa.outbox.relay.batch-size:100}") int batchSize) {
        this.relay = relay;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${homa.outbox.relay.fixed-delay:PT1S}")
    public void relayDueEvents() {
        relay.relayDue(batchSize);
    }
}

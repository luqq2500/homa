package com.homa.ie.port;

import java.time.Instant;

/**
 * The contract every integration event shares, whichever context publishes it. Each publisher's interface
 * ({@code CustomerIE}, {@code PaymentIE}, ...) extends it, and every record in {@code com.homa.ie.events} implements
 * one of those.
 *
 * <p>Delivery across contexts goes through the transactional outbox and is ordered per aggregate. The aggregate an
 * event is about is named by the record's component right after {@code eventId}: a String ending in {@code Id},
 * such as {@code reservationId}, {@code checkoutId} or {@code providerId}. Events about the same aggregate reach
 * their consumers in the order they were published.
 */
public interface IntegrationEvent {

    /**
     * Unique per event, assigned by the publisher. Handlers record (eventId, handler) in a ProcessedMessageStore
     * so a redelivered event is a no-op.
     */
    String eventId();

    /** When the fact happened, as decided by the publishing context's clock. */
    Instant occurredAt();
}

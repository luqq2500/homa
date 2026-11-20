package com.homa.ie.port;

import java.time.Instant;

/**
 * Every integration event the Listing context publishes. The context's single IE publisher takes this interface.
 */
public interface ListingIE {

    /**
     * Unique per event, assigned by the publisher. Handlers record (eventId, handler) in a ProcessedMessageStore
     * so a redelivered event is a no-op.
     */
    String eventId();

    /** When the fact happened, as decided by the publishing context's clock. */
    Instant occurredAt();
}

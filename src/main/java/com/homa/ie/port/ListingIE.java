package com.homa.ie.port;

import java.time.Instant;

/**
 * Every integration event the Listing context publishes. The context's single IE publisher takes this interface.
 */
public interface ListingIE {

    /** When the fact happened, as decided by the publishing context's clock. */
    Instant occurredAt();
}

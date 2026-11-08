package ie.port;

import java.time.Instant;

/** An integration event published by offering: listings going live, changing, and pausing. */
public interface OfferingIE {

    /** When the event happened. */
    Instant occurredAt();
}

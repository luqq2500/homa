package ie.port;

import java.time.Instant;

/** An integration event published by the offering context. */
public interface OfferingIE {

    Instant occurredAt();
}

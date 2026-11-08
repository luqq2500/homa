package ie.port;

import java.time.Instant;

/** An integration event published by policy: changes to the Homa-set values. */
public interface PolicyIE {

    /** When the event happened. */
    Instant occurredAt();
}

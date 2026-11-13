package ie.port;

import java.time.Instant;

/** An integration event published by resolution: the admins' decisions. */
public interface ResolutionIE {

    /** When the event happened. */
    Instant occurredAt();
}

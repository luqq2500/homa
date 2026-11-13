package ie.port;

import java.time.Instant;

/** An integration event published by the resolution context. */
public interface ResolutionIE {

    Instant occurredAt();
}

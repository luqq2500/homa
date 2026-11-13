package ie.port;

import java.time.Instant;

/** An integration event published by the availability context. */
public interface AvailabilityIE {

    Instant occurredAt();
}

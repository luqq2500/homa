package ie.port;

import java.time.Instant;

/** An integration event published by discovery: searches and how many providers matched. */
public interface DiscoveryIE {

    /** When the event happened. */
    Instant occurredAt();
}

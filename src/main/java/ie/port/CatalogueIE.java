package ie.port;

import java.time.Instant;

/** An integration event published by catalogue: changes to Homa's job and tool lists. */
public interface CatalogueIE {

    /** When the event happened. */
    Instant occurredAt();
}

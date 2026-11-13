package ie.port;

import java.time.Instant;

/** An integration event published by the catalogue context. */
public interface CatalogueIE {

    Instant occurredAt();
}

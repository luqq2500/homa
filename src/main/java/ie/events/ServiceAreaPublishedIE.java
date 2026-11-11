package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** Homa set the cities it serves; providers cover cities from this list. It carries every city, so listeners keep a full copy. */
public record ServiceAreaPublishedIE(
        Set<String> cities,
        Instant occurredAt) implements CatalogueIE {

    public ServiceAreaPublishedIE {
        cities = Set.copyOf(cities);
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}

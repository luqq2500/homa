package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;
import java.util.Objects;

/** Homa renamed a job; its code stays, so listings and reservations keep it. Listeners that show the name update it. */
public record JobRenamedIE(
        String jobCode,
        String name,
        Instant occurredAt) implements CatalogueIE {

    public JobRenamedIE {
        Objects.requireNonNull(jobCode, "jobCode");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}

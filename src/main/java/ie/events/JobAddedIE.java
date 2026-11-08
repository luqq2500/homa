package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;
import java.util.Objects;

/** Homa added a job to its job list; providers can now offer it. */
public record JobAddedIE(
        String jobCode,
        String name,
        String group,
        Instant occurredAt) implements CatalogueIE {

    public JobAddedIE {
        Objects.requireNonNull(jobCode, "jobCode");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}

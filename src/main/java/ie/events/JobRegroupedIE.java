package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;
import java.util.Objects;

/** Homa moved a job to another display group, such as from Kitchen to Living room; its code stays. */
public record JobRegroupedIE(
        String jobCode,
        String group,
        Instant occurredAt) implements CatalogueIE {

    public JobRegroupedIE {
        Objects.requireNonNull(jobCode, "jobCode");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}

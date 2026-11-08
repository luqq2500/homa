package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;
import java.util.Objects;

/** A job was retired from Homa's job list; providers can no longer offer it. */
public record JobRetiredIE(
        String jobCode,
        Instant occurredAt) implements CatalogueIE {

    public JobRetiredIE {
        Objects.requireNonNull(jobCode, "jobCode");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}

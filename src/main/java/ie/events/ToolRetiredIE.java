package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;
import java.util.Objects;

/** A tool was retired from Homa's tool list; providers can no longer say they bring it. */
public record ToolRetiredIE(
        String toolCode,
        Instant occurredAt) implements CatalogueIE {

    public ToolRetiredIE {
        Objects.requireNonNull(toolCode, "toolCode");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}

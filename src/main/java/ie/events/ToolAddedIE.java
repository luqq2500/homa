package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;
import java.util.Objects;

/** Homa added a tool to its tool list; providers can now say they bring it. */
public record ToolAddedIE(
        String toolCode,
        String name,
        Instant occurredAt) implements CatalogueIE {

    public ToolAddedIE {
        Objects.requireNonNull(toolCode, "toolCode");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}

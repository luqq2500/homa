package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;
import java.util.Objects;

/** Homa renamed a tool; its code stays, so listings keep it. Listeners that show the name update it. */
public record ToolRenamedIE(
        String toolCode,
        String name,
        Instant occurredAt) implements CatalogueIE {

    public ToolRenamedIE {
        Objects.requireNonNull(toolCode, "toolCode");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}

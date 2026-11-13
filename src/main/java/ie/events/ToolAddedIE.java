package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;

/** Homa added a tool that providers can now bring. */
public record ToolAddedIE(String toolId, String name, Instant occurredAt) implements CatalogueIE {}

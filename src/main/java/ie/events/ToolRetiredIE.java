package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;

/** Homa retired a tool: providers can no longer pick it, and it comes off every listing. */
public record ToolRetiredIE(String toolId, String reason, Instant occurredAt) implements CatalogueIE {}

package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;

/** Homa added a job that providers can now offer. */
public record JobAddedIE(String jobId, String name, String groupId, Instant occurredAt) implements CatalogueIE {}

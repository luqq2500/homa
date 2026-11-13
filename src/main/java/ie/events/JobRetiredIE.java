package ie.events;

import ie.port.CatalogueIE;
import java.time.Instant;

/** Homa retired a job: providers can no longer offer it, and it comes off every listing. */
public record JobRetiredIE(String jobId, String reason, Instant occurredAt) implements CatalogueIE {}

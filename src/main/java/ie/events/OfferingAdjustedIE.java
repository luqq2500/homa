package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;

/** Homa adjusted a provider's offering (a retired job or tool, or narrowed hours); the provider is told. */
public record OfferingAdjustedIE(String providerId, String adjustment, Instant occurredAt) implements OfferingIE {}

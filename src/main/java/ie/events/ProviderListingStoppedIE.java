package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;

/** A live provider stopped being live: taken offline, or paused because the business lapsed. */
public record ProviderListingStoppedIE(String providerId, String reason, Instant occurredAt) implements OfferingIE {}

package ie.events;

import ie.port.AccountIE;
import java.time.Instant;

/** A business registered with its phone number; it is the provider from here on. */
public record BusinessRegisteredIE(String businessId, String language, Instant occurredAt) implements AccountIE {}

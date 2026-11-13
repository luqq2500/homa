package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** The business missed its re-verification: its listing is paused until it passes. */
public record BusinessLapsedIE(String businessId, Instant occurredAt) implements VerificationIE {}

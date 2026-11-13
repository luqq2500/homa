package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** The KYC provider cleared the serious flag on the business. */
public record BusinessFlagClearedIE(String businessId, Instant occurredAt) implements VerificationIE {}

package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** The KYC provider cleared the serious flag on the business; it is back where it stood, verified or not. */
public record BusinessFlagClearedIE(String businessId, boolean verified, Instant occurredAt) implements VerificationIE {}

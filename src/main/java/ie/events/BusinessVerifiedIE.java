package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** The business is verified until this date: after its KYB, a re-verification, a date brought forward, or a cleared flag. */
public record BusinessVerifiedIE(String businessId, String ssmNumber, String registeredName, Instant verifiedUntil, Instant lastVerifiedAt, Instant occurredAt) implements VerificationIE {}

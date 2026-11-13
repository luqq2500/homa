package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** The business's KYB was rejected; it is told to redo it. */
public record BusinessKybRejectedIE(String businessId, Instant occurredAt) implements VerificationIE {}

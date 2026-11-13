package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** The KYC provider confirmed a serious flag on the business; it lapses at once. */
public record BusinessSeriouslyFlaggedIE(String businessId, Instant occurredAt) implements VerificationIE {}

package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** A team member passed a re-verification. */
public record TeamMemberReverifiedIE(String memberId, String businessId, Instant lastVerifiedAt, Instant occurredAt) implements VerificationIE {}

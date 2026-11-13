package ie.events;

import ie.port.VerificationIE;
import java.time.Instant;

/** A verified and confirmed person joined the business's team. */
public record TeamMemberJoinedIE(String memberId, String businessId, String role, String registeredName, String profilePhoto, Instant lastVerifiedAt, Instant occurredAt) implements VerificationIE {}
